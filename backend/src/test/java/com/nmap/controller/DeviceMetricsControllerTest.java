package com.nmap.controller;

import com.nmap.dto.DeviceHealthState;
import com.nmap.dto.DeviceMetricsDto;
import com.nmap.dto.DeviceMetricsHistoryPointDto;
import com.nmap.dto.InterfaceMetricsDto;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.service.DeviceMetricsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceMetricsController.class)
@Import(GlobalExceptionHandler.class)
class DeviceMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceMetricsProvider metricsProvider;

    private DeviceMetricsDto sampleMetrics;
    private List<DeviceMetricsHistoryPointDto> sampleHistory;

    @BeforeEach
    void setUp() {
        sampleMetrics = new DeviceMetricsDto(
                1L,
                "core-rtr01",
                Instant.now(),
                42.5,
                55.0,
                14.2,
                0.0,
                DeviceHealthState.HEALTHY,
                List.of(new InterfaceMetricsDto("GigabitEthernet0/0/0", 125.4, 88.1, 1000L, 12.5)),
                true,
                "SIMULATED_PROVIDER_PHASE5",
                "Simulated telemetry data for demonstration and monitoring UI visualization. Not real physical SNMP telemetry."
        );

        sampleHistory = List.of(
                new DeviceMetricsHistoryPointDto(Instant.parse("2026-09-29T10:00:00Z"), 40.0, 50.0, 12.0, 100.0, 80.0),
                new DeviceMetricsHistoryPointDto(Instant.parse("2026-09-29T10:01:00Z"), 45.0, 52.0, 15.0, 110.0, 85.0)
        );
    }

    @Test
    @DisplayName("GET /api/devices/{id}/metrics should return 200 with simulated telemetry")
    void testGetDeviceMetricsSuccess() throws Exception {
        when(metricsProvider.getDeviceMetrics(1L)).thenReturn(sampleMetrics);

        mockMvc.perform(get("/api/devices/1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId", is(1)))
                .andExpect(jsonPath("$.deviceHostname", is("core-rtr01")))
                .andExpect(jsonPath("$.isSimulated", is(true)))
                .andExpect(jsonPath("$.healthState", is("HEALTHY")))
                .andExpect(jsonPath("$.cpuUtilizationPercent", is(42.5)))
                .andExpect(jsonPath("$.memoryUtilizationPercent", is(55.0)))
                .andExpect(jsonPath("$.disclaimer", containsString("Simulated telemetry data")))
                .andExpect(jsonPath("$.interfaceMetrics", hasSize(1)))
                .andExpect(jsonPath("$.interfaceMetrics[0].interfaceName", is("GigabitEthernet0/0/0")));

        verify(metricsProvider, times(1)).getDeviceMetrics(1L);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/metrics should return 404 when device not found")
    void testGetDeviceMetricsNotFound() throws Exception {
        when(metricsProvider.getDeviceMetrics(99L))
                .thenThrow(new ResourceNotFoundException("NetworkDevice", "id", 99L));

        mockMvc.perform(get("/api/devices/99/metrics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("NetworkDevice not found with id: '99'")));

        verify(metricsProvider, times(1)).getDeviceMetrics(99L);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/metrics/history should return 200 with history points")
    void testGetDeviceMetricsHistorySuccess() throws Exception {
        when(metricsProvider.getDeviceMetricsHistory(1L, 2)).thenReturn(sampleHistory);

        mockMvc.perform(get("/api/devices/1/metrics/history?points=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cpuPercent", is(40.0)))
                .andExpect(jsonPath("$[1].cpuPercent", is(45.0)));

        verify(metricsProvider, times(1)).getDeviceMetricsHistory(1L, 2);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/metrics/history should return 400 when points count exceeds 60")
    void testGetDeviceMetricsHistoryPointsExceeded() throws Exception {
        when(metricsProvider.getDeviceMetricsHistory(1L, 100))
                .thenThrow(new IllegalArgumentException("History points requested must be between 1 and 60"));

        mockMvc.perform(get("/api/devices/1/metrics/history?points=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("must be between 1 and 60")));

        verify(metricsProvider, times(1)).getDeviceMetricsHistory(1L, 100);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/metrics/history should return 404 when device not found")
    void testGetDeviceMetricsHistoryNotFound() throws Exception {
        when(metricsProvider.getDeviceMetricsHistory(99L, 20))
                .thenThrow(new ResourceNotFoundException("NetworkDevice", "id", 99L));

        mockMvc.perform(get("/api/devices/99/metrics/history?points=20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("NetworkDevice not found with id: '99'")));

        verify(metricsProvider, times(1)).getDeviceMetricsHistory(99L, 20);
    }
}
