package com.nmap.service;

import com.nmap.dto.DeviceHealthState;
import com.nmap.dto.DeviceMetricsDto;
import com.nmap.dto.DeviceMetricsHistoryPointDto;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.NetworkDevice;
import com.nmap.entity.NetworkInterface;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.service.impl.SimulatedDeviceMetricsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceMetricsServiceTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private NetworkInterfaceRepository interfaceRepository;

    @InjectMocks
    private SimulatedDeviceMetricsProvider metricsProvider;

    private NetworkDevice sampleDevice;
    private NetworkInterface sampleInterface;

    @BeforeEach
    void setUp() {
        sampleDevice = new NetworkDevice("core-rtr01", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router");
        sampleDevice.setId(1L);

        sampleInterface = new NetworkInterface();
        sampleInterface.setId(10L);
        sampleInterface.setDevice(sampleDevice);
        sampleInterface.setInterfaceName("GigabitEthernet0/0/0");
        sampleInterface.setSpeedMbps(1000L);
    }

    @Test
    @DisplayName("Should return valid simulated metrics with explicit disclaimer")
    void testGetDeviceMetricsSuccess() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(interfaceRepository.findByDeviceId(1L)).thenReturn(List.of(sampleInterface));

        DeviceMetricsDto metrics = metricsProvider.getDeviceMetrics(1L);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getDeviceId()).isEqualTo(1L);
        assertThat(metrics.getDeviceHostname()).isEqualTo("core-rtr01");
        assertThat(metrics.isSimulated()).isTrue();
        assertThat(metrics.getDisclaimer()).contains("Simulated telemetry data");
        assertThat(metrics.getTelemetrySource()).contains("SIMULATED_PROVIDER");
        assertThat(metrics.getCpuUtilizationPercent()).isBetween(0.0, 100.0);
        assertThat(metrics.getMemoryUtilizationPercent()).isBetween(0.0, 100.0);
        assertThat(metrics.getLatencyMs()).isGreaterThan(0.0);
        assertThat(metrics.getHealthState()).isIn(DeviceHealthState.HEALTHY, DeviceHealthState.WARNING, DeviceHealthState.DEGRADED);
        assertThat(metrics.getInterfaceMetrics()).isNotEmpty();
        assertThat(metrics.getInterfaceMetrics().get(0).getInterfaceName()).isEqualTo("GigabitEthernet0/0/0");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when device does not exist")
    void testGetDeviceMetricsDeviceNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricsProvider.getDeviceMetrics(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkDevice not found with id: '99'");
    }

    @Test
    @DisplayName("Should return ordered historical points ascending in time")
    void testGetDeviceMetricsHistorySuccess() {
        when(deviceRepository.existsById(1L)).thenReturn(true);

        List<DeviceMetricsHistoryPointDto> history = metricsProvider.getDeviceMetricsHistory(1L, 10);

        assertThat(history).hasSize(10);
        for (int i = 0; i < history.size() - 1; i++) {
            assertThat(history.get(i).getTimestamp()).isBefore(history.get(i + 1).getTimestamp());
            assertThat(history.get(i).getCpuPercent()).isBetween(0.0, 100.0);
            assertThat(history.get(i).getMemoryPercent()).isBetween(0.0, 100.0);
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when history points is less than 1")
    void testGetDeviceMetricsHistoryPointsTooLow() {
        when(deviceRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> metricsProvider.getDeviceMetricsHistory(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between 1 and 60");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when history points exceeds maximum of 60")
    void testGetDeviceMetricsHistoryPointsTooHigh() {
        when(deviceRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> metricsProvider.getDeviceMetricsHistory(1L, 65))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between 1 and 60");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when requesting history for non-existent device")
    void testGetDeviceMetricsHistoryDeviceNotFound() {
        when(deviceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> metricsProvider.getDeviceMetricsHistory(99L, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkDevice not found with id: '99'");
    }
}
