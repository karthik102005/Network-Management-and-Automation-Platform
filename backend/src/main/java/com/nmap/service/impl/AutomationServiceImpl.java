package com.nmap.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.*;
import com.nmap.entity.AutomationChangeRequest;
import com.nmap.entity.AutomationStatus;
import com.nmap.entity.ConfigFormat;
import com.nmap.entity.DeviceConfiguration;
import com.nmap.entity.NetworkDevice;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.AutomationChangeRequestRepository;
import com.nmap.repository.DeviceConfigurationRepository;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.service.AutomationService;
import com.nmap.service.DeviceConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AutomationServiceImpl implements AutomationService {

    private static final Logger log = LoggerFactory.getLogger(AutomationServiceImpl.class);

    private static final Pattern FORBIDDEN_SHELL_PATTERNS = Pattern.compile(
            "(\\||;|&&|\\|\\||`|\\$\\(|sudo|rm\\s+-rf|bash|sh\\s+-c|chmod|powershell)",
            Pattern.CASE_INSENSITIVE
    );

    private final AutomationChangeRequestRepository changeRequestRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final DeviceConfigurationRepository configurationRepository;
    private final DeviceConfigurationService configurationService;
    private final ObjectMapper objectMapper;

    private final List<AutomationPlaybookDto> playbooks = new ArrayList<>();

    public AutomationServiceImpl(AutomationChangeRequestRepository changeRequestRepository,
                                 NetworkDeviceRepository deviceRepository,
                                 DeviceConfigurationRepository configurationRepository,
                                 DeviceConfigurationService configurationService,
                                 ObjectMapper objectMapper) {
        this.changeRequestRepository = changeRequestRepository;
        this.deviceRepository = deviceRepository;
        this.configurationRepository = configurationRepository;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
        this.objectMapper.findAndRegisterModules();
        initializePlaybooks();
    }

    private void initializePlaybooks() {
        playbooks.add(new AutomationPlaybookDto(
                "playbook-vlan-provision",
                "VLAN & Subnet Provisioning",
                "SWITCHING",
                "CISCO",
                "Automates the creation of a Layer 2 VLAN and Layer 3 SVI management gateway.",
                List.of("VLAN_ID", "VLAN_NAME", "IP_ADDRESS", "SUBNET_MASK"),
                "vlan {{VLAN_ID}}\n name {{VLAN_NAME}}\nexit\ninterface Vlan{{VLAN_ID}}\n description SVI for {{VLAN_NAME}}\n ip address {{IP_ADDRESS}} {{SUBNET_MASK}}\n no shutdown\nexit"
        ));

        playbooks.add(new AutomationPlaybookDto(
                "playbook-acl-hardening",
                "Management Access Control List (ACL)",
                "SECURITY",
                "CISCO",
                "Restricts interactive terminal lines (VTY) to authorized administrative subnets.",
                List.of("ACL_NAME", "MANAGEMENT_SUBNET", "WILDCARD_MASK"),
                "ip access-list standard {{ACL_NAME}}\n permit {{MANAGEMENT_SUBNET}} {{WILDCARD_MASK}}\n deny any log\nexit\nline vty 0 4\n access-class {{ACL_NAME}} in\nexit"
        ));

        playbooks.add(new AutomationPlaybookDto(
                "playbook-ntp-syslog",
                "NTP & Remote Syslog Baseline Hardening",
                "MANAGEMENT",
                "CISCO",
                "Enforces network time synchronization and centralized logging for compliance.",
                List.of("NTP_SERVER", "SYSLOG_HOST"),
                "ntp server {{NTP_SERVER}}\nlogging host {{SYSLOG_HOST}}\nservice timestamps log datetime msec\nservice timestamps debug datetime msec\nexit"
        ));

        playbooks.add(new AutomationPlaybookDto(
                "playbook-interface-turnup",
                "Interface Configuration & Turn-Up",
                "INTERFACE",
                "CISCO",
                "Configures interface description, operational parameters, and enables link administratively.",
                List.of("INTERFACE_NAME", "DESCRIPTION", "IP_ADDRESS", "SUBNET_MASK"),
                "interface {{INTERFACE_NAME}}\n description {{DESCRIPTION}}\n ip address {{IP_ADDRESS}} {{SUBNET_MASK}}\n no shutdown\nexit"
        ));
    }

    @Override
    public List<AutomationPlaybookDto> getPlaybooks() {
        return Collections.unmodifiableList(playbooks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationChangeRequestResponseDto> getAllChangeRequests() {
        return changeRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationChangeRequestResponseDto> getChangeRequestsByDeviceId(Long deviceId) {
        verifyDeviceExists(deviceId);
        return changeRequestRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationChangeRequestResponseDto getChangeRequestById(Long id) {
        AutomationChangeRequest request = changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationChangeRequest", "id", id));
        return mapToResponseDto(request);
    }

    @Override
    @Transactional
    public AutomationChangeRequestResponseDto createChangeRequest(AutomationChangeRequestCreateDto createDto) {
        NetworkDevice device = deviceRepository.findById(createDto.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", createDto.getDeviceId()));

        // Validate command safety and syntax
        validateConfigurationCommands(createDto.getConfigCommands());

        // Inspect currently active configuration snapshot version (safeguard 7)
        Optional<DeviceConfiguration> activeConfig = configurationRepository.findByDeviceIdAndActiveTrue(device.getId()).stream().findFirst();
        Integer preChangeVersion = activeConfig.map(DeviceConfiguration::getVersion).orElse(null);

        AutomationChangeRequest entity = new AutomationChangeRequest(
                device,
                createDto.getTitle().trim(),
                createDto.getDescription() != null ? createDto.getDescription().trim() : null,
                createDto.getPlaybookId(),
                createDto.getConfigCommands().trim(),
                createDto.getAuthor() != null ? createDto.getAuthor().trim() : "operator",
                preChangeVersion
        );

        AutomationChangeRequest saved = changeRequestRepository.save(entity);
        log.info("Created new automation change request id={} in DRAFT status for device id={} ({})",
                saved.getId(), device.getId(), device.getHostname());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public AutomationChangeRequestResponseDto executeChangeRequest(Long id) {
        AutomationChangeRequest request = changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationChangeRequest", "id", id));

        // Safeguard 8: Status transition validation
        if (request.getStatus() == AutomationStatus.COMPLETED) {
            throw new IllegalStateException("Change request " + id + " is already COMPLETED and cannot be executed again.");
        }
        if (request.getStatus() == AutomationStatus.ROLLED_BACK) {
            throw new IllegalStateException("Change request " + id + " has been ROLLED_BACK and cannot be re-executed.");
        }
        if (request.getStatus() != AutomationStatus.DRAFT && request.getStatus() != AutomationStatus.VALIDATING) {
            throw new IllegalStateException("Cannot execute change request in status: " + request.getStatus());
        }

        NetworkDevice device = request.getDevice();
        List<AutomationExecutionStepDto> steps = new ArrayList<>();
        Instant startTime = Instant.now();
        request.setExecutedAt(startTime);

        // Safeguard 7: Detect stale configuration before execution
        Optional<DeviceConfiguration> currentActive = configurationRepository.findByDeviceIdAndActiveTrue(device.getId()).stream().findFirst();
        Integer currentActiveVersion = currentActive.map(DeviceConfiguration::getVersion).orElse(null);

        if (!Objects.equals(request.getPreChangeVersion(), currentActiveVersion)) {
            request.setStatus(AutomationStatus.FAILED);
            String staleMsg = String.format(
                    "Stale change request detected: The device active configuration has changed from version %s to version %s since this request was created. Execution rejected to protect device state.",
                    request.getPreChangeVersion(), currentActiveVersion
            );
            request.setErrorMessage(staleMsg);
            steps.add(new AutomationExecutionStepDto(1, "STALE_CHECK", "FAILED", staleMsg, Instant.now()));
            request.setExecutionLog(serializeSteps(steps));
            changeRequestRepository.save(request);
            throw new IllegalStateException(staleMsg);
        }

        // Step 1: Simulated Pre-Check (No real ICMP/TCP probes, per safeguard 3)
        steps.add(new AutomationExecutionStepDto(
                1,
                "SIMULATED_PRE_CHECK",
                "PASSED",
                String.format("Device %s (IP: %s) verified in inventory. Status: %s. Simulated reachability check passed.",
                        device.getHostname(), device.getManagementIp(), device.getStatus()),
                Instant.now()
        ));

        // Step 2: Educational Syntax Validation
        try {
            validateConfigurationCommands(request.getConfigCommands());
            steps.add(new AutomationExecutionStepDto(
                    2,
                    "SYNTAX_VALIDATION",
                    "PASSED",
                    "Basic educational syntax validation passed (Cisco IOS simulation). Prohibited shell patterns absent.",
                    Instant.now()
            ));
        } catch (IllegalArgumentException ex) {
            request.setStatus(AutomationStatus.FAILED);
            request.setErrorMessage("Syntax validation failed: " + ex.getMessage());
            steps.add(new AutomationExecutionStepDto(2, "SYNTAX_VALIDATION", "FAILED", ex.getMessage(), Instant.now()));
            request.setExecutionLog(serializeSteps(steps));
            changeRequestRepository.save(request);
            throw ex;
        }

        // Step 3: Simulated Deployment & Transcript Generation
        String transcript = generateSimulatedTranscript(device.getHostname(), request.getConfigCommands());
        request.setTerminalTranscript(transcript);
        steps.add(new AutomationExecutionStepDto(
                3,
                "SIMULATED_DEPLOYMENT",
                "PASSED",
                String.format("Delivered %d configuration commands to %s CLI in-memory simulator.",
                        countCommandLines(request.getConfigCommands()), device.getHostname()),
                Instant.now()
        ));

        // Step 4: Configuration Snapshot Commit (reusing DeviceConfigurationService, safeguard 2)
        String baseConfig = currentActive.map(DeviceConfiguration::getConfigText)
                .orElse("! Baseline configuration for " + device.getHostname() + "\nhostname " + device.getHostname() + "\n");
        String mergedConfig = baseConfig + "\n! --- Automated Change: " + request.getTitle() + " ---\n" + request.getConfigCommands() + "\n";

        DeviceConfigurationRequestDto configRequest = new DeviceConfigurationRequestDto(
                mergedConfig,
                ConfigFormat.CISCO_IOS,
                request.getAuthor(),
                "Automated Change: " + request.getTitle()
        );

        DeviceConfigurationResponseDto committedSnapshot = configurationService.createConfiguration(device.getId(), configRequest);
        request.setPostChangeVersion(committedSnapshot.getVersion());
        steps.add(new AutomationExecutionStepDto(
                4,
                "SNAPSHOT_COMMIT",
                "PASSED",
                String.format("Created persistent configuration snapshot v%d (Checksum: %s).",
                        committedSnapshot.getVersion(), committedSnapshot.getChecksum()),
                Instant.now()
        ));

        // Step 5: Post-Verification
        steps.add(new AutomationExecutionStepDto(
                5,
                "POST_VERIFICATION",
                "PASSED",
                String.format("Post-change compliance verified. Active configuration version is now v%d.",
                        committedSnapshot.getVersion()),
                Instant.now()
        ));

        request.setStatus(AutomationStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setExecutionLog(serializeSteps(steps));

        AutomationChangeRequest saved = changeRequestRepository.save(request);
        log.info("Executed automation change request id={} successfully. Created configuration version {}",
                saved.getId(), committedSnapshot.getVersion());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public AutomationChangeRequestResponseDto rollbackChangeRequest(Long id) {
        AutomationChangeRequest request = changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationChangeRequest", "id", id));

        // Safeguard 8: Status checks
        if (request.getStatus() == AutomationStatus.ROLLED_BACK) {
            throw new IllegalStateException("Change request " + id + " has already been rolled back.");
        }
        if (request.getStatus() != AutomationStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED change requests can be rolled back. Current status: " + request.getStatus());
        }

        if (request.getPreChangeVersion() == null || request.getPreChangeVersion() < 1) {
            throw new IllegalStateException("Cannot roll back change request " + id + ": No valid pre-change configuration version was recorded.");
        }

        NetworkDevice device = request.getDevice();

        // Safeguard 7: Detect stale configuration before rollback
        Optional<DeviceConfiguration> currentActive = configurationRepository.findByDeviceIdAndActiveTrue(device.getId()).stream().findFirst();
        Integer currentActiveVersion = currentActive.map(DeviceConfiguration::getVersion).orElse(null);

        if (!Objects.equals(currentActiveVersion, request.getPostChangeVersion())) {
            throw new IllegalStateException(String.format(
                    "Cannot roll back change request %d: Current active configuration version (%s) does not match post-change version (%s). An intervening configuration may have been applied.",
                    id, currentActiveVersion, request.getPostChangeVersion()
            ));
        }

        // Restore configuration using existing restore service (Safeguards 2 & 8)
        DeviceConfigurationResponseDto restoredConfig = configurationService.restoreConfiguration(
                device.getId(),
                request.getPreChangeVersion()
        );

        request.setRollbackVersion(restoredConfig.getVersion());
        request.setStatus(AutomationStatus.ROLLED_BACK);

        // Append rollback step to execution log
        List<AutomationExecutionStepDto> steps = deserializeSteps(request.getExecutionLog());
        steps.add(new AutomationExecutionStepDto(
                steps.size() + 1,
                "ROLLBACK_RESTORE",
                "PASSED",
                String.format("Rolled back to pre-change configuration v%d by creating new restoration version v%d.",
                        request.getPreChangeVersion(), restoredConfig.getVersion()),
                Instant.now()
        ));
        request.setExecutionLog(serializeSteps(steps));

        // Append rollback message to transcript
        String rollbackTranscript = (request.getTerminalTranscript() != null ? request.getTerminalTranscript() : "")
                + String.format("\n\n! --- AUTOMATED ROLLBACK INITIATED ---\n%s# configure replace flash:config-v%d.cfg\nRestoring configuration to version %d...\n[OK] Configuration restored as Version %d.\n",
                device.getHostname(), request.getPreChangeVersion(), request.getPreChangeVersion(), restoredConfig.getVersion());
        request.setTerminalTranscript(rollbackTranscript);

        AutomationChangeRequest saved = changeRequestRepository.save(request);
        log.info("Rolled back automation change request id={} on device id={}. New active version: v{}",
                saved.getId(), device.getId(), restoredConfig.getVersion());
        return mapToResponseDto(saved);
    }

    private void validateConfigurationCommands(String commands) {
        if (commands == null || commands.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration commands cannot be blank.");
        }

        if (FORBIDDEN_SHELL_PATTERNS.matcher(commands).find()) {
            throw new IllegalArgumentException("Configuration commands contain prohibited shell characters or unsafe commands.");
        }

        // Basic educational validation
        String[] lines = commands.split("\\r?\\n");
        boolean hasValidDirective = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("!")) {
                continue;
            }
            hasValidDirective = true;
            break;
        }

        if (!hasValidDirective) {
            throw new IllegalArgumentException("Configuration commands must contain at least one valid directive line.");
        }
    }

    private String generateSimulatedTranscript(String hostname, String commands) {
        StringBuilder sb = new StringBuilder();
        sb.append(hostname).append("# configure terminal\n");
        sb.append("Enter configuration commands, one per line. End with CNTL/Z.\n");

        String[] lines = commands.split("\\r?\\n");
        String currentPrompt = hostname + "(config)# ";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            sb.append(currentPrompt).append(trimmed).append("\n");

            if (trimmed.startsWith("interface ") || trimmed.startsWith("vlan ") || trimmed.startsWith("router ") || trimmed.startsWith("line ")) {
                currentPrompt = hostname + "(config-sub)# ";
            } else if (trimmed.equals("exit") || trimmed.equals("end")) {
                currentPrompt = hostname + "(config)# ";
            }
        }

        sb.append(hostname).append("(config)# end\n");
        sb.append(hostname).append("# write memory\n");
        sb.append("Building configuration...\n");
        sb.append("[OK] Configuration committed to simulated non-volatile storage.\n");
        return sb.toString();
    }

    private int countCommandLines(String commands) {
        if (commands == null || commands.trim().isEmpty()) return 0;
        int count = 0;
        for (String line : commands.split("\\r?\\n")) {
            if (!line.trim().isEmpty() && !line.trim().startsWith("!")) count++;
        }
        return count;
    }

    private void verifyDeviceExists(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("NetworkDevice", "id", deviceId);
        }
    }

    private String serializeSteps(List<AutomationExecutionStepDto> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize execution steps to JSON", e);
            return "[]";
        }
    }

    private List<AutomationExecutionStepDto> deserializeSteps(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AutomationExecutionStepDto>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize execution steps from JSON", e);
            return new ArrayList<>();
        }
    }

    private AutomationChangeRequestResponseDto mapToResponseDto(AutomationChangeRequest entity) {
        return new AutomationChangeRequestResponseDto(
                entity.getId(),
                entity.getDevice() != null ? entity.getDevice().getId() : null,
                entity.getDevice() != null ? entity.getDevice().getHostname() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getPlaybookId(),
                entity.getStatus(),
                entity.getConfigCommands(),
                entity.getPreChangeVersion(),
                entity.getPostChangeVersion(),
                entity.getRollbackVersion(),
                entity.getAuthor(),
                deserializeSteps(entity.getExecutionLog()),
                entity.getTerminalTranscript(),
                entity.getErrorMessage(),
                Boolean.TRUE.equals(entity.getIsSimulated()),
                "Simulated automation execution. No live hardware commands issued.",
                entity.getCreatedAt(),
                entity.getExecutedAt(),
                entity.getCompletedAt()
        );
    }
}
