package com.nmap.service.impl;

import com.nmap.dto.*;
import com.nmap.entity.ConfigFormat;
import com.nmap.entity.DeviceConfiguration;
import com.nmap.entity.NetworkDevice;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.DeviceConfigurationRepository;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.service.DeviceConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class DeviceConfigurationServiceImpl implements DeviceConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceConfigurationServiceImpl.class);

    private final DeviceConfigurationRepository configurationRepository;
    private final NetworkDeviceRepository deviceRepository;

    public DeviceConfigurationServiceImpl(DeviceConfigurationRepository configurationRepository,
                                         NetworkDeviceRepository deviceRepository) {
        this.configurationRepository = configurationRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceConfigurationResponseDto> getConfigurationsByDeviceId(Long deviceId) {
        verifyDeviceExists(deviceId);
        List<DeviceConfiguration> configs = configurationRepository.findByDeviceIdOrderByVersionDesc(deviceId);
        return configs.stream().map(this::mapToResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceConfigurationResponseDto getConfigurationByVersion(Long deviceId, Integer version) {
        verifyDeviceExists(deviceId);
        DeviceConfiguration config = configurationRepository.findByDeviceIdAndVersion(deviceId, version)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceConfiguration", "version", version));
        return mapToResponseDto(config);
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDto createConfiguration(Long deviceId, DeviceConfigurationRequestDto requestDto) {
        NetworkDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", deviceId));

        // Determine next version number
        int nextVersion = configurationRepository.findTopByDeviceIdOrderByVersionDesc(deviceId)
                .map(last -> last.getVersion() + 1)
                .orElse(1);

        // Deactivate previous active configurations for this device
        List<DeviceConfiguration> activeConfigs = configurationRepository.findByDeviceIdAndActiveTrue(deviceId);
        for (DeviceConfiguration activeConfig : activeConfigs) {
            activeConfig.setActive(false);
        }
        if (!activeConfigs.isEmpty()) {
            configurationRepository.saveAll(activeConfigs);
        }

        String checksum = calculateSha256(requestDto.getConfigText());
        String author = (requestDto.getAuthor() != null && !requestDto.getAuthor().isBlank())
                ? requestDto.getAuthor().trim()
                : "operator";

        DeviceConfiguration newConfig = new DeviceConfiguration(
                device,
                nextVersion,
                requestDto.getConfigText(),
                requestDto.getConfigFormat(),
                checksum,
                true,
                author,
                requestDto.getDescription()
        );

        DeviceConfiguration saved = configurationRepository.save(newConfig);
        log.info("Created new configuration snapshot id={} v{} for device id={} ({})",
                saved.getId(), saved.getVersion(), deviceId, device.getHostname());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public DeviceConfigurationResponseDto restoreConfiguration(Long deviceId, Integer versionToRestore) {
        NetworkDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", deviceId));

        DeviceConfiguration targetToRestore = configurationRepository.findByDeviceIdAndVersion(deviceId, versionToRestore)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceConfiguration", "version", versionToRestore));

        // Determine next version number (strictly incremented, preserving historical versions)
        int nextVersion = configurationRepository.findTopByDeviceIdOrderByVersionDesc(deviceId)
                .map(last -> last.getVersion() + 1)
                .orElse(1);

        // Deactivate previous active configurations
        List<DeviceConfiguration> activeConfigs = configurationRepository.findByDeviceIdAndActiveTrue(deviceId);
        for (DeviceConfiguration activeConfig : activeConfigs) {
            activeConfig.setActive(false);
        }
        if (!activeConfigs.isEmpty()) {
            configurationRepository.saveAll(activeConfigs);
        }

        String restoreDesc = "Restored from version " + versionToRestore +
                (targetToRestore.getDescription() != null ? " (" + targetToRestore.getDescription() + ")" : "");

        DeviceConfiguration restoredSnapshot = new DeviceConfiguration(
                device,
                nextVersion,
                targetToRestore.getConfigText(),
                targetToRestore.getConfigFormat(),
                targetToRestore.getChecksum(),
                true,
                "operator (restored)",
                restoreDesc
        );

        DeviceConfiguration saved = configurationRepository.save(restoredSnapshot);
        log.info("Restored configuration version {} by creating new version {} for device id={}",
                versionToRestore, nextVersion, deviceId);

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigurationDiffResponseDto compareConfigurations(Long deviceId, Integer v1, Integer v2) {
        NetworkDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", deviceId));

        DeviceConfiguration config1 = configurationRepository.findByDeviceIdAndVersion(deviceId, v1)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceConfiguration", "version", v1));

        DeviceConfiguration config2 = configurationRepository.findByDeviceIdAndVersion(deviceId, v2)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceConfiguration", "version", v2));

        return computeDiff(device, config1, config2);
    }

    @Override
    @Transactional
    public void deleteConfigurationsByDeviceId(Long deviceId) {
        log.info("Deleting all stored configuration snapshots for device id={}", deviceId);
        configurationRepository.deleteByDeviceId(deviceId);
    }

    @Override
    public List<ConfigurationTemplateDto> getTemplates() {
        return List.of(
                new ConfigurationTemplateDto(
                        "cisco-ios-router-baseline",
                        "Cisco IOS Router Baseline (Example)",
                        "CISCO",
                        "ROUTER",
                        ConfigFormat.CISCO_IOS,
                        "Standard base configuration with hostname, domain lookup disable, management loopback, and console timeout.",
                        """
                        ! Cisco IOS Router Baseline Configuration (Example)
                        ! NOTE: Generic educational template - not validated on physical hardware.
                        service timestamps debug datetime msec
                        service timestamps log datetime msec
                        no service password-encryption
                        !
                        hostname {{HOSTNAME}}
                        !
                        ip domain-lookup
                        ip domain-name lab.nmap.local
                        !
                        interface Loopback0
                         description Management Loopback
                         ip address {{MGMT_IP}} 255.255.255.255
                         no shutdown
                        !
                        line con 0
                         exec-timeout 15 0
                         logging synchronous
                        line vty 0 4
                         transport input ssh
                         exec-timeout 15 0
                        !
                        end
                        """.stripIndent(),
                        "This template is a generic educational example and has not been tested or validated against physical hardware.",
                        List.of("HOSTNAME", "MGMT_IP")
                ),
                new ConfigurationTemplateDto(
                        "cisco-ios-ospf-example",
                        "Cisco IOS OSPF Single-Area (Example)",
                        "CISCO",
                        "ROUTER",
                        ConfigFormat.CISCO_IOS,
                        "OSPFv2 single-area backbone routing configuration for core routers.",
                        """
                        ! Cisco IOS OSPF Area 0 Configuration (Example)
                        ! NOTE: Generic educational template - not validated on physical hardware.
                        router ospf 1
                         router-id {{MGMT_IP}}
                         log-adjacency-changes
                         passive-interface Loopback0
                         network 192.168.10.0 0.0.0.255 area 0
                        !
                        interface GigabitEthernet0/0/0
                         ip ospf hello-interval 10
                         ip ospf dead-interval 40
                        !
                        end
                        """.stripIndent(),
                        "This template is a generic educational example and has not been tested or validated against physical hardware.",
                        List.of("MGMT_IP")
                ),
                new ConfigurationTemplateDto(
                        "cisco-l2-switch-baseline",
                        "Cisco L2 Switch Baseline (Example)",
                        "CISCO",
                        "SWITCH",
                        ConfigFormat.CISCO_IOS,
                        "Layer 2 access switch template with Rapid Spanning Tree, management SVI, and access port defaults.",
                        """
                        ! Cisco L2 Switch Baseline Configuration (Example)
                        ! NOTE: Generic educational template - not validated on physical hardware.
                        hostname {{HOSTNAME}}
                        !
                        spanning-tree mode rapid-pvst
                        spanning-tree portfast default
                        !
                        vlan 10
                         name CORPORATE_DATA
                        vlan 20
                         name MANAGEMENT
                        !
                        interface Vlan20
                         description Management SVI
                         ip address {{MGMT_IP}} 255.255.255.0
                         no shutdown
                        !
                        interface GigabitEthernet0/1
                         switchport mode access
                         switchport access vlan 10
                         spanning-tree portfast
                        !
                        end
                        """.stripIndent(),
                        "This template is a generic educational example and has not been tested or validated against physical hardware.",
                        List.of("HOSTNAME", "MGMT_IP")
                ),
                new ConfigurationTemplateDto(
                        "linux-gateway-baseline",
                        "Linux Gateway Baseline (Example)",
                        "LINUX",
                        "GATEWAY",
                        ConfigFormat.TEXT,
                        "Linux network gateway configuration with IP forwarding sysctl settings and interface definitions.",
                        """
                        # /etc/sysctl.d/99-network-gateway.conf (Example)
                        # NOTE: Generic educational template - not validated on physical hardware.
                        net.ipv4.ip_forward = 1
                        net.ipv4.conf.all.rp_filter = 1
                        net.ipv4.conf.default.rp_filter = 1
                        net.ipv4.conf.all.accept_redirects = 0
                        net.ipv4.icmp_echo_ignore_broadcasts = 1

                        # Interface Configuration for Hostname: {{HOSTNAME}}
                        # Management IP: {{MGMT_IP}}/24
                        auto eth0
                        iface eth0 inet static
                            address {{MGMT_IP}}/24
                            dns-nameservers 8.8.8.8 1.1.1.1
                        """.stripIndent(),
                        "This template is a generic educational example and has not been tested or validated against physical hardware.",
                        List.of("HOSTNAME", "MGMT_IP")
                )
        );
    }

    private void verifyDeviceExists(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("NetworkDevice", "id", deviceId);
        }
    }

    private DeviceConfigurationResponseDto mapToResponseDto(DeviceConfiguration entity) {
        return new DeviceConfigurationResponseDto(
                entity.getId(),
                entity.getDevice().getId(),
                entity.getDevice().getHostname(),
                entity.getVersion(),
                entity.getConfigText(),
                entity.getConfigFormat(),
                entity.getChecksum(),
                entity.getActive(),
                entity.getAuthor(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public static String calculateSha256(String text) {
        if (text == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private ConfigurationDiffResponseDto computeDiff(NetworkDevice device,
                                                     DeviceConfiguration config1,
                                                     DeviceConfiguration config2) {
        String[] lines1 = (config1.getConfigText() != null && !config1.getConfigText().isEmpty())
                ? config1.getConfigText().split("\\r?\\n")
                : new String[0];
        String[] lines2 = (config2.getConfigText() != null && !config2.getConfigText().isEmpty())
                ? config2.getConfigText().split("\\r?\\n")
                : new String[0];

        boolean identical = Objects.equals(config1.getChecksum(), config2.getChecksum())
                || Arrays.equals(lines1, lines2);

        List<ConfigurationDiffLineDto> diffLines = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int unchanged = 0;

        if (identical) {
            for (int i = 0; i < lines1.length; i++) {
                diffLines.add(new ConfigurationDiffLineDto(DiffLineType.UNCHANGED, i + 1, i + 1, lines1[i]));
                unchanged++;
            }
        } else {
            // LCS-based Diff algorithm
            int n = lines1.length;
            int m = lines2.length;
            int[][] dp = new int[n + 1][m + 1];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (lines1[i].equals(lines2[j])) {
                        dp[i + 1][j + 1] = dp[i][j] + 1;
                    } else {
                        dp[i + 1][j + 1] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                    }
                }
            }

            int i = n;
            int j = m;
            List<ConfigurationDiffLineDto> reversedDiff = new ArrayList<>();

            while (i > 0 || j > 0) {
                if (i > 0 && j > 0 && lines1[i - 1].equals(lines2[j - 1])) {
                    reversedDiff.add(new ConfigurationDiffLineDto(DiffLineType.UNCHANGED, i, j, lines1[i - 1]));
                    unchanged++;
                    i--;
                    j--;
                } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                    reversedDiff.add(new ConfigurationDiffLineDto(DiffLineType.ADDED, null, j, lines2[j - 1]));
                    added++;
                    j--;
                } else if (i > 0) {
                    reversedDiff.add(new ConfigurationDiffLineDto(DiffLineType.REMOVED, i, null, lines1[i - 1]));
                    removed++;
                    i--;
                }
            }

            Collections.reverse(reversedDiff);
            diffLines = reversedDiff;
        }

        return new ConfigurationDiffResponseDto(
                device.getId(),
                device.getHostname(),
                config1.getVersion(),
                config2.getVersion(),
                identical,
                lines1.length,
                lines2.length,
                added,
                removed,
                unchanged,
                diffLines
        );
    }
}
