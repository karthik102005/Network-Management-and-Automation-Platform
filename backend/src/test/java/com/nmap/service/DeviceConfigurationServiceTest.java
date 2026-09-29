package com.nmap.service;

import com.nmap.dto.ConfigurationDiffResponseDto;
import com.nmap.dto.ConfigurationTemplateDto;
import com.nmap.dto.DeviceConfigurationRequestDto;
import com.nmap.dto.DeviceConfigurationResponseDto;
import com.nmap.dto.DiffLineType;
import com.nmap.entity.*;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.DeviceConfigurationRepository;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.service.impl.DeviceConfigurationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceConfigurationServiceTest {

    @Mock
    private DeviceConfigurationRepository configurationRepository;

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @InjectMocks
    private DeviceConfigurationServiceImpl configurationService;

    private NetworkDevice sampleDevice;
    private DeviceConfiguration sampleConfigV1;
    private DeviceConfiguration sampleConfigV2;

    @BeforeEach
    void setUp() {
        sampleDevice = new NetworkDevice("core-rtr01", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router");
        sampleDevice.setId(1L);

        sampleConfigV1 = new DeviceConfiguration(
                sampleDevice,
                1,
                "hostname core-rtr01\ninterface GigabitEthernet0/0/0\nip address 192.168.10.1 255.255.255.0",
                ConfigFormat.CISCO_IOS,
                DeviceConfigurationServiceImpl.calculateSha256("hostname core-rtr01\ninterface GigabitEthernet0/0/0\nip address 192.168.10.1 255.255.255.0"),
                false,
                "admin",
                "Initial baseline config"
        );
        sampleConfigV1.setId(10L);
        sampleConfigV1.setCreatedAt(Instant.now().minusSeconds(3600));

        sampleConfigV2 = new DeviceConfiguration(
                sampleDevice,
                2,
                "hostname core-rtr01\ninterface GigabitEthernet0/0/0\nip address 192.168.10.1 255.255.255.0\ninterface Loopback0\nip address 10.0.0.1 255.255.255.255",
                ConfigFormat.CISCO_IOS,
                DeviceConfigurationServiceImpl.calculateSha256("hostname core-rtr01\ninterface GigabitEthernet0/0/0\nip address 192.168.10.1 255.255.255.0\ninterface Loopback0\nip address 10.0.0.1 255.255.255.255"),
                true,
                "admin",
                "Added Loopback0"
        );
        sampleConfigV2.setId(20L);
        sampleConfigV2.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create initial configuration snapshot with version 1 and active true")
    void testCreateInitialConfiguration() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(configurationRepository.findTopByDeviceIdOrderByVersionDesc(1L)).thenReturn(Optional.empty());
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of());

        DeviceConfigurationRequestDto request = new DeviceConfigurationRequestDto(
                "hostname core-rtr01\nno ip domain-lookup",
                ConfigFormat.CISCO_IOS,
                "operator",
                "Baseline"
        );

        when(configurationRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> {
            DeviceConfiguration cfg = invocation.getArgument(0);
            cfg.setId(100L);
            cfg.setCreatedAt(Instant.now());
            return cfg;
        });

        DeviceConfigurationResponseDto response = configurationService.createConfiguration(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getConfigFormat()).isEqualTo(ConfigFormat.CISCO_IOS);
        assertThat(response.getChecksum()).isEqualTo(DeviceConfigurationServiceImpl.calculateSha256(request.getConfigText()));
        assertThat(response.getAuthor()).isEqualTo("operator");
        assertThat(response.getDeviceId()).isEqualTo(1L);
        assertThat(response.getDeviceHostname()).isEqualTo("core-rtr01");
    }

    @Test
    @DisplayName("Should auto-increment version and deactivate previous active snapshots")
    void testCreateSubsequentConfiguration() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(configurationRepository.findTopByDeviceIdOrderByVersionDesc(1L)).thenReturn(Optional.of(sampleConfigV1));
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(sampleConfigV1));

        DeviceConfigurationRequestDto request = new DeviceConfigurationRequestDto(
                "hostname core-rtr01\ninterface Loopback0",
                ConfigFormat.CISCO_IOS,
                "admin",
                "Version 2"
        );

        when(configurationRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> {
            DeviceConfiguration cfg = invocation.getArgument(0);
            cfg.setId(101L);
            cfg.setCreatedAt(Instant.now());
            return cfg;
        });

        DeviceConfigurationResponseDto response = configurationService.createConfiguration(1L, request);

        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getActive()).isTrue();
        assertThat(sampleConfigV1.getActive()).isFalse();
        verify(configurationRepository, times(1)).saveAll(List.of(sampleConfigV1));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating configuration for non-existent device")
    void testCreateConfigurationDeviceNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        DeviceConfigurationRequestDto request = new DeviceConfigurationRequestDto("test", ConfigFormat.TEXT, "admin", "test");

        assertThatThrownBy(() -> configurationService.createConfiguration(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkDevice not found with id: '99'");
    }

    @Test
    @DisplayName("Should retrieve configurations by device ordered by version descending")
    void testGetConfigurationsByDeviceId() {
        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(configurationRepository.findByDeviceIdOrderByVersionDesc(1L)).thenReturn(List.of(sampleConfigV2, sampleConfigV1));

        List<DeviceConfigurationResponseDto> result = configurationService.getConfigurationsByDeviceId(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(2);
        assertThat(result.get(1).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should retrieve single configuration by version")
    void testGetConfigurationByVersion() {
        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(configurationRepository.findByDeviceIdAndVersion(1L, 1)).thenReturn(Optional.of(sampleConfigV1));

        DeviceConfigurationResponseDto result = configurationService.getConfigurationByVersion(1L, 1);

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getConfigText()).isEqualTo(sampleConfigV1.getConfigText());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when configuration version does not exist")
    void testGetConfigurationByVersionNotFound() {
        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(configurationRepository.findByDeviceIdAndVersion(1L, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configurationService.getConfigurationByVersion(1L, 99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DeviceConfiguration not found with version: '99'");
    }

    @Test
    @DisplayName("Should restore historical version by creating new version while preserving existing versions")
    void testRestoreConfigurationPreservesHistory() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(configurationRepository.findByDeviceIdAndVersion(1L, 1)).thenReturn(Optional.of(sampleConfigV1));
        when(configurationRepository.findTopByDeviceIdOrderByVersionDesc(1L)).thenReturn(Optional.of(sampleConfigV2));
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(sampleConfigV2));

        when(configurationRepository.save(any(DeviceConfiguration.class))).thenAnswer(invocation -> {
            DeviceConfiguration cfg = invocation.getArgument(0);
            cfg.setId(30L);
            cfg.setCreatedAt(Instant.now());
            return cfg;
        });

        DeviceConfigurationResponseDto restored = configurationService.restoreConfiguration(1L, 1);

        assertThat(restored.getVersion()).isEqualTo(3);
        assertThat(restored.getActive()).isTrue();
        assertThat(restored.getConfigText()).isEqualTo(sampleConfigV1.getConfigText());
        assertThat(restored.getChecksum()).isEqualTo(sampleConfigV1.getChecksum());
        assertThat(restored.getDescription()).contains("Restored from version 1");
        assertThat(sampleConfigV2.getActive()).isFalse();

        // Verify version 1 and 2 were not deleted or modified
        verify(configurationRepository, never()).delete(any());
        verify(configurationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should compare identical configurations and return identical=true")
    void testCompareConfigurationsIdentical() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(configurationRepository.findByDeviceIdAndVersion(1L, 1)).thenReturn(Optional.of(sampleConfigV1));

        DeviceConfiguration identicalCopy = new DeviceConfiguration(
                sampleDevice, 3, sampleConfigV1.getConfigText(), ConfigFormat.CISCO_IOS,
                sampleConfigV1.getChecksum(), true, "admin", "copy"
        );
        when(configurationRepository.findByDeviceIdAndVersion(1L, 3)).thenReturn(Optional.of(identicalCopy));

        ConfigurationDiffResponseDto diff = configurationService.compareConfigurations(1L, 1, 3);

        assertThat(diff.isIdentical()).isTrue();
        assertThat(diff.getAddedCount()).isEqualTo(0);
        assertThat(diff.getRemovedCount()).isEqualTo(0);
        assertThat(diff.getUnchangedCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should compare different configurations and correctly detect added, removed, and unchanged lines")
    void testCompareConfigurationsWithChanges() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(configurationRepository.findByDeviceIdAndVersion(1L, 1)).thenReturn(Optional.of(sampleConfigV1));
        when(configurationRepository.findByDeviceIdAndVersion(1L, 2)).thenReturn(Optional.of(sampleConfigV2));

        ConfigurationDiffResponseDto diff = configurationService.compareConfigurations(1L, 1, 2);

        assertThat(diff.isIdentical()).isFalse();
        assertThat(diff.getV1()).isEqualTo(1);
        assertThat(diff.getV2()).isEqualTo(2);
        assertThat(diff.getAddedCount()).isGreaterThan(0);
        assertThat(diff.getDiffLines()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return built-in example templates with explicit example warnings")
    void testGetTemplates() {
        List<ConfigurationTemplateDto> templates = configurationService.getTemplates();

        assertThat(templates).hasSize(4);
        assertThat(templates).extracting(ConfigurationTemplateDto::getId)
                .containsExactlyInAnyOrder(
                        "cisco-ios-router-baseline",
                        "cisco-ios-ospf-example",
                        "cisco-l2-switch-baseline",
                        "linux-gateway-baseline"
                );
        for (ConfigurationTemplateDto template : templates) {
            assertThat(template.getExampleWarning()).isNotBlank();
            assertThat(template.getName()).contains("Example");
            assertThat(template.getTemplateText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Should delete configurations by device id cleanly")
    void testDeleteConfigurationsByDeviceId() {
        configurationService.deleteConfigurationsByDeviceId(1L);
        verify(configurationRepository, times(1)).deleteByDeviceId(1L);
    }
}
