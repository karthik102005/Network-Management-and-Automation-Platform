package com.nmap.service;

import com.nmap.dto.AlertResponseDto;
import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.entity.*;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkAlertRepository;
import com.nmap.service.impl.AlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AlertServiceTest {

    @Mock
    private NetworkAlertRepository alertRepository;

    @InjectMocks
    private AlertServiceImpl alertService;

    private NetworkDevice sampleDevice;
    private NetworkAlert sampleAlert;

    @BeforeEach
    void setUp() {
        sampleDevice = new NetworkDevice("core-rtr01", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Main Router");
        sampleDevice.setId(1L);

        sampleAlert = new NetworkAlert(
                sampleDevice,
                AlertType.DEVICE_UNREACHABLE,
                AlertSeverity.CRITICAL,
                AlertStatus.OPEN,
                "Device timed out on ICMP and management ports",
                "REACHABILITY_MONITOR"
        );
        sampleAlert.setId(10L);
        sampleAlert.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create DEVICE_UNREACHABLE alert when probe result is UNREACHABLE and no active alert exists")
    void testProcessReachabilityResultCreatesAlert() {
        when(alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                eq(1L), eq(AlertType.DEVICE_UNREACHABLE), any()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(NetworkAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceReachabilityResponseDto unreachableResult = new DeviceReachabilityResponseDto(
                1L, "core-rtr01", "192.168.10.1", false, "UNREACHABLE", "NONE", null, null,
                Instant.now(), "Target did not respond within timeout"
        );

        alertService.processReachabilityResult(sampleDevice, unreachableResult);

        verify(alertRepository, times(1)).save(argThat(alert ->
                alert.getDevice().getId().equals(1L) &&
                alert.getAlertType() == AlertType.DEVICE_UNREACHABLE &&
                alert.getSeverity() == AlertSeverity.CRITICAL &&
                alert.getStatus() == AlertStatus.OPEN
        ));
    }

    @Test
    @DisplayName("Should suppress duplicate active alerts when device already has an OPEN alert")
    void testProcessReachabilityResultSuppressesDuplicateAlert() {
        when(alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                eq(1L), eq(AlertType.DEVICE_UNREACHABLE), any()))
                .thenReturn(Optional.of(sampleAlert));

        DeviceReachabilityResponseDto unreachableResult = new DeviceReachabilityResponseDto(
                1L, "core-rtr01", "192.168.10.1", false, "UNREACHABLE", "NONE", null, null,
                Instant.now(), "Target did not respond within timeout"
        );

        alertService.processReachabilityResult(sampleDevice, unreachableResult);

        // Verify save is never called because duplicate active alert exists
        verify(alertRepository, never()).save(any(NetworkAlert.class));
    }

    @Test
    @DisplayName("Should auto-resolve active alerts when a subsequent reachability check confirms device is reachable")
    void testProcessReachabilityResultAutoResolvesActiveAlert() {
        when(alertRepository.findByDeviceIdAndStatusIn(eq(1L), any()))
                .thenReturn(List.of(sampleAlert));

        DeviceReachabilityResponseDto reachableResult = new DeviceReachabilityResponseDto(
                1L, "core-rtr01", "192.168.10.1", true, "REACHABLE", "TCP_CONNECTION", 22, 18L,
                Instant.now(), "Target responded on port 22 in 18ms"
        );

        alertService.processReachabilityResult(sampleDevice, reachableResult);

        verify(alertRepository, times(1)).save(argThat(alert ->
                alert.getStatus() == AlertStatus.RESOLVED &&
                alert.getResolvedAt() != null &&
                alert.getMessage().contains("Auto-recovered")
        ));
    }

    @Test
    @DisplayName("Should NOT create alerts for INVALID_TARGET or INCONCLUSIVE probe results")
    void testProcessReachabilityResultIgnoresInvalidOrInconclusive() {
        DeviceReachabilityResponseDto invalidResult = new DeviceReachabilityResponseDto(
                1L, "core-rtr01", "999.1.1.1", false, "INVALID_TARGET", "NONE", null, null,
                Instant.now(), "Invalid target IP address"
        );

        alertService.processReachabilityResult(sampleDevice, invalidResult);

        verify(alertRepository, never()).save(any(NetworkAlert.class));

        DeviceReachabilityResponseDto inconclusiveResult = new DeviceReachabilityResponseDto(
                1L, "core-rtr01", "192.168.10.1", false, "INCONCLUSIVE", "NONE", null, null,
                Instant.now(), "DNS lookup failed"
        );

        alertService.processReachabilityResult(sampleDevice, inconclusiveResult);

        verify(alertRepository, never()).save(any(NetworkAlert.class));
    }

    @Test
    @DisplayName("Should acknowledge OPEN alert and record acknowledged timestamp")
    void testAcknowledgeAlertSuccess() {
        when(alertRepository.findById(10L)).thenReturn(Optional.of(sampleAlert));
        when(alertRepository.save(any(NetworkAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertResponseDto dto = alertService.acknowledgeAlert(10L);

        assertThat(dto.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(dto.getAcknowledgedAt()).isNotNull();
        verify(alertRepository, times(1)).save(sampleAlert);
    }

    @Test
    @DisplayName("Should reject acknowledgement of an already RESOLVED alert with IllegalArgumentException")
    void testAcknowledgeResolvedAlertThrowsException() {
        sampleAlert.setStatus(AlertStatus.RESOLVED);
        when(alertRepository.findById(10L)).thenReturn(Optional.of(sampleAlert));

        assertThatThrownBy(() -> alertService.acknowledgeAlert(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot acknowledge an already resolved alert");

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should resolve OPEN or ACKNOWLEDGED alert and record resolved timestamp")
    void testResolveAlertSuccess() {
        when(alertRepository.findById(10L)).thenReturn(Optional.of(sampleAlert));
        when(alertRepository.save(any(NetworkAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertResponseDto dto = alertService.resolveAlert(10L);

        assertThat(dto.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(dto.getResolvedAt()).isNotNull();
        verify(alertRepository, times(1)).save(sampleAlert);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when alert does not exist")
    void testGetAlertByIdNotFound() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getAlertById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkAlert not found with id: '999'");
    }

    @Test
    @DisplayName("Should delete all alerts belonging to a specific device during cascade cleanup")
    void testDeleteAlertsByDeviceId() {
        when(alertRepository.findByDeviceId(1L)).thenReturn(List.of(sampleAlert));

        alertService.deleteAlertsByDeviceId(1L);

        verify(alertRepository, times(1)).deleteAll(List.of(sampleAlert));
    }
}
