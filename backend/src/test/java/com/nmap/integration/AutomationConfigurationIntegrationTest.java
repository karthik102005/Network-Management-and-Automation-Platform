package com.nmap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.AutomationChangeRequestCreateDto;
import com.nmap.dto.DeviceConfigurationRequestDto;
import com.nmap.entity.*;
import com.nmap.repository.AutomationChangeRequestRepository;
import com.nmap.repository.DeviceConfigurationRepository;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.service.DeviceConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 8.1A: End-to-end integration test verifying real Spring service wiring,
 * JPA persistence, configuration snapshot versioning, and change orchestration
 * on the isolated in-memory H2 test database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutomationConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkDeviceRepository deviceRepository;

    @Autowired
    private DeviceConfigurationRepository configurationRepository;

    @Autowired
    private AutomationChangeRequestRepository changeRequestRepository;

    @Autowired
    private DeviceConfigurationService configurationService;

    private NetworkDevice testDevice;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        // Seed dedicated test device
        NetworkDevice device = new NetworkDevice(
                "integration-rtr01",
                "192.168.200.1",
                DeviceType.ROUTER,
                DeviceVendor.CISCO,
                DeviceStatus.UP,
                "Integration Test Dedicated Router"
        );
        testDevice = deviceRepository.save(device);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        changeRequestRepository.deleteAll();
        configurationRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    @Test
    @DisplayName("Full Lifecycle: Draft -> Execute -> Snapshot Commit -> Rollback with non-destructive restore")
    void testFullAutomationLifecycle_DraftExecuteRollbackWithSnapshotPersistence() throws Exception {
        // 1. Seed baseline configuration v1 in H2
        String baseConfigText = "! Baseline Configuration for integration-rtr01\nhostname integration-rtr01\ninterface GigabitEthernet0/0/0\n ip address 192.168.200.1 255.255.255.0\n no shutdown\nend\n";
        DeviceConfigurationRequestDto baseDto = new DeviceConfigurationRequestDto(
                baseConfigText, ConfigFormat.CISCO_IOS, "admin", "Initial baseline"
        );
        configurationService.createConfiguration(testDevice.getId(), baseDto);

        // Verify baseline state in DB
        List<DeviceConfiguration> initialConfigs = configurationRepository.findByDeviceIdOrderByVersionDesc(testDevice.getId());
        assertThat(initialConfigs).hasSize(1);
        assertThat(initialConfigs.get(0).getVersion()).isEqualTo(1);
        assertThat(initialConfigs.get(0).getActive()).isTrue();

        // 2. Draft Creation: POST /api/automation/requests
        String ntpCommands = "ntp server pool.ntp.org\nlogging host 192.168.200.50\nservice timestamps log datetime msec\nexit";
        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                testDevice.getId(),
                "Integration NTP & Logging Hardening",
                "Apply NTP and syslog hardening in integration test",
                "playbook-ntp-syslog",
                ntpCommands,
                "operator-integration"
        );

        MvcResult createResult = mockMvc.perform(post("/api/automation/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/automation/requests/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.preChangeVersion", is(1)))
                .andExpect(jsonPath("$.postChangeVersion", nullValue()))
                .andExpect(jsonPath("$.isSimulated", is(true)))
                .andReturn();

        Long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify Database after Draft: exactly 1 change request in DRAFT, NO new snapshot created!
        List<AutomationChangeRequest> drafts = changeRequestRepository.findByDeviceIdOrderByCreatedAtDesc(testDevice.getId());
        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getStatus()).isEqualTo(AutomationStatus.DRAFT);
        assertThat(drafts.get(0).getPreChangeVersion()).isEqualTo(1);
        assertThat(configurationRepository.findByDeviceIdOrderByVersionDesc(testDevice.getId())).hasSize(1);

        // 3. Execution: POST /api/automation/requests/{id}/execute
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(requestId.intValue())))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.preChangeVersion", is(1)))
                .andExpect(jsonPath("$.postChangeVersion", is(2)))
                .andExpect(jsonPath("$.executionSteps", hasSize(5)))
                .andExpect(jsonPath("$.executionSteps[0].stepName", is("SIMULATED_PRE_CHECK")))
                .andExpect(jsonPath("$.executionSteps[0].status", is("PASSED")))
                .andExpect(jsonPath("$.executionSteps[3].stepName", is("SNAPSHOT_COMMIT")))
                .andExpect(jsonPath("$.executionSteps[3].status", is("PASSED")))
                .andExpect(jsonPath("$.terminalTranscript", containsString("configure terminal")))
                .andExpect(jsonPath("$.terminalTranscript", containsString("write memory")));

        // Verify Database after Execution:
        // - Exactly 2 configuration snapshots exist
        // - Snapshot v1 is now active = false
        // - Snapshot v2 is active = true and contains appended automated change text
        List<DeviceConfiguration> postExecConfigs = configurationRepository.findByDeviceIdOrderByVersionDesc(testDevice.getId());
        assertThat(postExecConfigs).hasSize(2);

        DeviceConfiguration configV2 = postExecConfigs.get(0); // ordered by version desc
        DeviceConfiguration configV1 = postExecConfigs.get(1);

        assertThat(configV2.getVersion()).isEqualTo(2);
        assertThat(configV2.getActive()).isTrue();
        assertThat(configV2.getConfigText()).contains("ntp server pool.ntp.org");
        assertThat(configV2.getChecksum()).isEqualTo(calculateSha256(configV2.getConfigText()));

        assertThat(configV1.getVersion()).isEqualTo(1);
        assertThat(configV1.getActive()).isFalse();

        // 4. Rollback: POST /api/automation/requests/{id}/rollback
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/rollback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(requestId.intValue())))
                .andExpect(jsonPath("$.status", is("ROLLED_BACK")))
                .andExpect(jsonPath("$.rollbackVersion", is(3)))
                .andExpect(jsonPath("$.terminalTranscript", containsString("AUTOMATED ROLLBACK INITIATED")));

        // Verify Database after Rollback:
        // - Non-destructive: exactly 3 configuration snapshots exist (v1, v2, v3)
        // - Snapshot v1 and v2 are active = false
        // - Snapshot v3 is active = true and has content identical to v1
        List<DeviceConfiguration> postRollbackConfigs = configurationRepository.findByDeviceIdOrderByVersionDesc(testDevice.getId());
        assertThat(postRollbackConfigs).hasSize(3);

        DeviceConfiguration configV3 = postRollbackConfigs.get(0);
        assertThat(configV3.getVersion()).isEqualTo(3);
        assertThat(configV3.getActive()).isTrue();
        assertThat(configV3.getConfigText()).isEqualTo(configV1.getConfigText());
        assertThat(configV3.getChecksum()).isEqualTo(configV1.getChecksum());
        assertThat(configV3.getAuthor()).isEqualTo("operator (restored)");

        assertThat(postRollbackConfigs.get(1).getVersion()).isEqualTo(2);
        assertThat(postRollbackConfigs.get(1).getActive()).isFalse();

        assertThat(postRollbackConfigs.get(2).getVersion()).isEqualTo(1);
        assertThat(postRollbackConfigs.get(2).getActive()).isFalse();
    }

    @Test
    @DisplayName("Stale Configuration Detection: Rejects execution if active version advanced independently")
    void testStaleConfigurationDetection_AbortsExecutionAndProtectsState() throws Exception {
        // Seed baseline v1
        configurationService.createConfiguration(testDevice.getId(), new DeviceConfigurationRequestDto(
                "hostname integration-rtr01\n", ConfigFormat.CISCO_IOS, "admin", "v1"
        ));

        // Create draft based on v1 (preChangeVersion = 1)
        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                testDevice.getId(), "Stale Change Request", "Desc",
                "playbook-ntp-syslog", "ntp server 10.0.0.1\nexit", "operator"
        );

        MvcResult createResult = mockMvc.perform(post("/api/automation/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        Long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Independent administrative change creates v2 in H2
        configurationService.createConfiguration(testDevice.getId(), new DeviceConfigurationRequestDto(
                "hostname integration-rtr01\ninterface Loopback0\n ip address 10.1.1.1 255.255.255.255\n",
                ConfigFormat.CISCO_IOS, "sec-admin", "v2 independent"
        ));

        // Attempt to execute stale draft -> must return 409 Conflict
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/execute"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Stale change request detected")));

        // Verify DB: Spring @Transactional rolls back on IllegalStateException,
        // which prevents partial state corruption and preserves request in DRAFT status
        AutomationChangeRequest failedRequest = changeRequestRepository.findById(requestId).orElseThrow();
        assertThat(failedRequest.getStatus()).isEqualTo(AutomationStatus.DRAFT);

        List<DeviceConfiguration> activeConfigs = configurationRepository.findByDeviceIdAndActiveTrue(testDevice.getId());
        assertThat(activeConfigs).hasSize(1);
        assertThat(activeConfigs.get(0).getVersion()).isEqualTo(2);

        // Confirm NO v3 snapshot was generated
        assertThat(configurationRepository.findByDeviceIdOrderByVersionDesc(testDevice.getId())).hasSize(2);
    }

    @Test
    @DisplayName("Workflow State Guards: Rejects execution of COMPLETED or ROLLED_BACK requests")
    void testWorkflowStateGuards_RejectsDuplicateExecutionAndRollback() throws Exception {
        // Seed baseline v1
        configurationService.createConfiguration(testDevice.getId(), new DeviceConfigurationRequestDto(
                "hostname integration-rtr01\n", ConfigFormat.CISCO_IOS, "admin", "v1"
        ));

        // Create and execute request
        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                testDevice.getId(), "Guarded Change Request", "Desc",
                "playbook-ntp-syslog", "ntp server 10.0.0.1\nexit", "operator"
        );

        MvcResult createResult = mockMvc.perform(post("/api/automation/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        Long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // First execution -> 200 OK
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/execute"))
                .andExpect(status().isOk());

        // Duplicate execution -> 409 Conflict
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/execute"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already COMPLETED")));

        // First rollback -> 200 OK
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/rollback"))
                .andExpect(status().isOk());

        // Duplicate rollback -> 409 Conflict
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/rollback"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("has already been rolled back")));

        // Execution after rollback -> 409 Conflict
        mockMvc.perform(post("/api/automation/requests/" + requestId + "/execute"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("cannot be re-executed")));
    }

    @Test
    @DisplayName("Security & Validation: Rejects prohibited shell injection patterns and creates no DB records")
    void testForbiddenShellCommands_RejectedAtDraftCreationZeroMutation() throws Exception {
        // Seed baseline v1
        configurationService.createConfiguration(testDevice.getId(), new DeviceConfigurationRequestDto(
                "hostname integration-rtr01\n", ConfigFormat.CISCO_IOS, "admin", "v1"
        ));

        AutomationChangeRequestCreateDto maliciousDto = new AutomationChangeRequestCreateDto(
                testDevice.getId(),
                "Malicious Request",
                "Injection test",
                "playbook-ntp-syslog",
                "ntp server 10.0.0.1; rm -rf /",
                "malicious-user"
        );

        // Draft creation must fail with 400 Bad Request
        mockMvc.perform(post("/api/automation/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prohibited shell characters")));

        // Verify DB: zero change requests created, baseline v1 untouched
        assertThat(changeRequestRepository.count()).isEqualTo(0);
        List<DeviceConfiguration> configs = configurationRepository.findByDeviceIdOrderByVersionDesc(testDevice.getId());
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).getVersion()).isEqualTo(1);
        assertThat(configs.get(0).getActive()).isTrue();
    }
}
