package com.nmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.AutomationChangeRequestCreateDto;
import com.nmap.dto.AutomationChangeRequestResponseDto;
import com.nmap.dto.AutomationPlaybookDto;
import com.nmap.dto.DeviceConfigurationResponseDto;
import com.nmap.entity.*;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.AutomationChangeRequestRepository;
import com.nmap.repository.DeviceConfigurationRepository;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.service.impl.AutomationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTest {

    @Mock
    private AutomationChangeRequestRepository changeRequestRepository;

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private DeviceConfigurationRepository configurationRepository;

    @Mock
    private DeviceConfigurationService configurationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private AutomationServiceImpl automationService;

    private NetworkDevice sampleDevice;
    private DeviceConfiguration sampleActiveConfig;
    private AutomationChangeRequest sampleDraftRequest;

    @BeforeEach
    void setUp() {
        sampleDevice = new NetworkDevice("R1", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router");
        sampleDevice.setId(1L);

        sampleActiveConfig = new DeviceConfiguration(
                sampleDevice, 3, "hostname R1\ninterface GigabitEthernet0/0/0\nend",
                ConfigFormat.CISCO_IOS, "sha256samplehash", true, "admin", "Initial"
        );
        sampleActiveConfig.setId(30L);

        sampleDraftRequest = new AutomationChangeRequest(
                sampleDevice,
                "Provision Engineering VLAN",
                "Add VLAN 100",
                "playbook-vlan-provision",
                "vlan 100\n name Engineering\nexit",
                "operator",
                3
        );
        sampleDraftRequest.setId(10L);
    }

    @Test
    @DisplayName("Should return pre-configured automation playbooks")
    void testGetPlaybooks() {
        List<AutomationPlaybookDto> playbooks = automationService.getPlaybooks();
        assertThat(playbooks).isNotEmpty();
        assertThat(playbooks).anyMatch(p -> p.getId().equals("playbook-vlan-provision"));
        assertThat(playbooks).anyMatch(p -> p.getId().equals("playbook-acl-hardening"));
        assertThat(playbooks).anyMatch(p -> p.getId().equals("playbook-ntp-syslog"));
    }

    @Test
    @DisplayName("Should create change request in DRAFT status with pre-change version recorded")
    void testCreateChangeRequestSuccess() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(sampleActiveConfig));
        when(changeRequestRepository.save(any(AutomationChangeRequest.class))).thenAnswer(invocation -> {
            AutomationChangeRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                1L, "Provision Engineering VLAN", "Add VLAN 100",
                "playbook-vlan-provision", "vlan 100\n name Engineering\nexit", "operator"
        );

        AutomationChangeRequestResponseDto response = automationService.createChangeRequest(createDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(AutomationStatus.DRAFT);
        assertThat(response.getPreChangeVersion()).isEqualTo(3);
        assertThat(response.isSimulated()).isTrue();

        // Verify NO configuration snapshot was created on draft creation
        verify(configurationService, never()).createConfiguration(any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when target device does not exist")
    void testCreateChangeRequestDeviceNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                99L, "Title", "Desc", "playbook", "vlan 100", "operator"
        );

        assertThatThrownBy(() -> automationService.createChangeRequest(createDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkDevice not found with id: '99'");
    }

    @Test
    @DisplayName("Should reject prohibited shell command characters")
    void testCreateChangeRequestForbiddenShellCommands() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));

        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                1L, "Malicious Request", "Desc", "playbook", "vlan 100; rm -rf /", "operator"
        );

        assertThatThrownBy(() -> automationService.createChangeRequest(createDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prohibited shell characters");
    }

    @Test
    @DisplayName("Should execute change request successfully and create new configuration snapshot")
    void testExecuteChangeRequestSuccess() {
        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(sampleActiveConfig));

        DeviceConfigurationResponseDto newSnapshot = new DeviceConfigurationResponseDto(
                40L, 1L, "R1", 4, "merged config", ConfigFormat.CISCO_IOS, "newhash", true, "operator", "Automated Change", Instant.now()
        );
        when(configurationService.createConfiguration(eq(1L), any())).thenReturn(newSnapshot);
        when(changeRequestRepository.save(any(AutomationChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AutomationChangeRequestResponseDto result = automationService.executeChangeRequest(10L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AutomationStatus.COMPLETED);
        assertThat(result.getPostChangeVersion()).isEqualTo(4);
        assertThat(result.getExecutionSteps()).hasSize(5);
        assertThat(result.getTerminalTranscript()).contains("R1# configure terminal");
        assertThat(result.getTerminalTranscript()).contains("write memory");

        verify(configurationService, times(1)).createConfiguration(eq(1L), any());
    }

    @Test
    @DisplayName("Should reject execution if active configuration changed since request creation (stale check)")
    void testExecuteChangeRequestStaleConfiguration() {
        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));

        // Active configuration has advanced to version 4 (stale draft was based on version 3)
        DeviceConfiguration newerConfig = new DeviceConfiguration(
                sampleDevice, 4, "hostname R1\nend", ConfigFormat.CISCO_IOS, "hash4", true, "admin", "Newer"
        );
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(newerConfig));

        assertThatThrownBy(() -> automationService.executeChangeRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stale change request detected");

        assertThat(sampleDraftRequest.getStatus()).isEqualTo(AutomationStatus.FAILED);
        verify(configurationService, never()).createConfiguration(any(), any());
    }

    @Test
    @DisplayName("Should reject execution if change request is already COMPLETED")
    void testExecuteChangeRequestAlreadyCompleted() {
        sampleDraftRequest.setStatus(AutomationStatus.COMPLETED);
        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));

        assertThatThrownBy(() -> automationService.executeChangeRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already COMPLETED");

        verify(configurationService, never()).createConfiguration(any(), any());
    }

    @Test
    @DisplayName("Should rollback completed change request using restoreConfiguration")
    void testRollbackChangeRequestSuccess() {
        sampleDraftRequest.setStatus(AutomationStatus.COMPLETED);
        sampleDraftRequest.setPreChangeVersion(3);
        sampleDraftRequest.setPostChangeVersion(4);

        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));

        DeviceConfiguration activeConfigV4 = new DeviceConfiguration(
                sampleDevice, 4, "v4 config", ConfigFormat.CISCO_IOS, "hash4", true, "operator", "v4"
        );
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(activeConfigV4));

        DeviceConfigurationResponseDto restoredV5 = new DeviceConfigurationResponseDto(
                50L, 1L, "R1", 5, "restored v3 content", ConfigFormat.CISCO_IOS, "restoredhash", true, "operator (restored)", "Restored", Instant.now()
        );
        when(configurationService.restoreConfiguration(1L, 3)).thenReturn(restoredV5);
        when(changeRequestRepository.save(any(AutomationChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AutomationChangeRequestResponseDto result = automationService.rollbackChangeRequest(10L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AutomationStatus.ROLLED_BACK);
        assertThat(result.getRollbackVersion()).isEqualTo(5);
        assertThat(result.getTerminalTranscript()).contains("AUTOMATED ROLLBACK INITIATED");

        verify(configurationService, times(1)).restoreConfiguration(1L, 3);
    }

    @Test
    @DisplayName("Should reject rollback if change request is already ROLLED_BACK")
    void testRollbackChangeRequestAlreadyRolledBack() {
        sampleDraftRequest.setStatus(AutomationStatus.ROLLED_BACK);
        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));

        assertThatThrownBy(() -> automationService.rollbackChangeRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has already been rolled back");

        verify(configurationService, never()).restoreConfiguration(any(), any());
    }

    @Test
    @DisplayName("Should reject rollback if change request is still in DRAFT status")
    void testRollbackChangeRequestNotCompleted() {
        sampleDraftRequest.setStatus(AutomationStatus.DRAFT);
        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));

        assertThatThrownBy(() -> automationService.rollbackChangeRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only COMPLETED change requests can be rolled back");

        verify(configurationService, never()).restoreConfiguration(any(), any());
    }

    @Test
    @DisplayName("Should reject rollback if active configuration changed since execution")
    void testRollbackChangeRequestInterveningConfiguration() {
        sampleDraftRequest.setStatus(AutomationStatus.COMPLETED);
        sampleDraftRequest.setPreChangeVersion(3);
        sampleDraftRequest.setPostChangeVersion(4);

        when(changeRequestRepository.findById(10L)).thenReturn(Optional.of(sampleDraftRequest));

        // Active version has changed to version 5 independently!
        DeviceConfiguration activeConfigV5 = new DeviceConfiguration(
                sampleDevice, 5, "v5 config", ConfigFormat.CISCO_IOS, "hash5", true, "another-admin", "v5"
        );
        when(configurationRepository.findByDeviceIdAndActiveTrue(1L)).thenReturn(List.of(activeConfigV5));

        assertThatThrownBy(() -> automationService.rollbackChangeRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match post-change version");

        verify(configurationService, never()).restoreConfiguration(any(), any());
    }
}
