package com.nmap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.entity.*;
import com.nmap.network.DeviceReachabilityService;
import com.nmap.repository.NetworkAlertRepository;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.service.AlertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 8.1B: Integration test verifying Reachability-to-Alert lifecycle,
 * deduplication, auto-recovery, and false-positive prevention on the isolated
 * in-memory H2 test database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReachabilityAlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkDeviceRepository deviceRepository;

    @Autowired
    private NetworkAlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    @MockitoBean
    private DeviceReachabilityService reachabilityService;

    private NetworkDevice testDevice;
    private final List<Long> createdDeviceIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cleanTestRecords();

        NetworkDevice device = new NetworkDevice(
                "reachability-test-rtr01",
                "192.168.100.1",
                DeviceType.ROUTER,
                DeviceVendor.CISCO,
                DeviceStatus.UP,
                "Reachability Alert Integration Test Router"
        );
        testDevice = deviceRepository.save(device);
        createdDeviceIds.add(testDevice.getId());
    }

    @AfterEach
    void tearDown() {
        cleanTestRecords();
    }

    private void cleanTestRecords() {
        for (Long deviceId : createdDeviceIds) {
            try {
                alertService.deleteAlertsByDeviceId(deviceId);
                if (deviceRepository.existsById(deviceId)) {
                    deviceRepository.deleteById(deviceId);
                }
            } catch (Exception ignored) {
            }
        }
        createdDeviceIds.clear();
    }

    @Test
    @DisplayName("Confirmed UNREACHABLE probe creates and persists a CRITICAL DEVICE_UNREACHABLE alert in H2")
    void testConfirmedUnreachable_CreatesAndPersistsCriticalAlert() throws Exception {
        DeviceReachabilityResponseDto unreachableResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                false,
                "UNREACHABLE",
                "NONE",
                null,
                null,
                Instant.now(),
                "Target device did not respond to ICMP echo or TCP probes within 2000ms."
        );

        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(unreachableResponse);

        // Trigger on-demand reachability probe
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId", is(testDevice.getId().intValue())))
                .andExpect(jsonPath("$.reachable", is(false)))
                .andExpect(jsonPath("$.reachabilityStatus", is("UNREACHABLE")));

        // Verify persistence in H2
        List<NetworkAlert> alerts = alertRepository.findByDeviceId(testDevice.getId());
        assertThat(alerts).hasSize(1);

        NetworkAlert alert = alerts.get(0);
        assertThat(alert.getAlertType()).isEqualTo(AlertType.DEVICE_UNREACHABLE);
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getSource()).isEqualTo("REACHABILITY_MONITOR");
        assertThat(alert.getMessage()).contains("Target device did not respond to ICMP echo or TCP probes within 2000ms.");
        assertThat(alert.getCreatedAt()).isNotNull();
        assertThat(alert.getResolvedAt()).isNull();
        assertThat(alert.getAcknowledgedAt()).isNull();
        assertThat(alert.getDevice().getId()).isEqualTo(testDevice.getId());
    }

    @Test
    @DisplayName("Repeated UNREACHABLE probes suppress duplicate alerts in both OPEN and ACKNOWLEDGED statuses")
    void testRepeatedUnreachable_SuppressesDuplicateAlertsInOpenAndAcknowledgedStatus() throws Exception {
        DeviceReachabilityResponseDto unreachableResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                false,
                "UNREACHABLE",
                "NONE",
                null,
                null,
                Instant.now(),
                "Target device did not respond to ICMP echo or TCP probes within 2000ms."
        );

        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(unreachableResponse);

        // Probe 1: Initial failure creates 1 OPEN alert
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachabilityStatus", is("UNREACHABLE")));

        List<NetworkAlert> alertsAfterFirst = alertRepository.findByDeviceId(testDevice.getId());
        assertThat(alertsAfterFirst).hasSize(1);
        Long alertId = alertsAfterFirst.get(0).getId();
        Instant createdAt = alertsAfterFirst.get(0).getCreatedAt();
        assertThat(alertsAfterFirst.get(0).getStatus()).isEqualTo(AlertStatus.OPEN);

        // Probe 2 & 3: Repeated failures while alert is OPEN must NOT create duplicate alerts
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk());

        List<NetworkAlert> alertsAfterRepeats = alertRepository.findByDeviceId(testDevice.getId());
        assertThat(alertsAfterRepeats).hasSize(1);
        assertThat(alertsAfterRepeats.get(0).getId()).isEqualTo(alertId);
        assertThat(alertsAfterRepeats.get(0).getCreatedAt()).isEqualTo(createdAt);

        // Acknowledge the alert via REST endpoint
        mockMvc.perform(patch("/api/alerts/{id}/acknowledge", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACKNOWLEDGED")))
                .andExpect(jsonPath("$.acknowledgedAt", notNullValue()));

        NetworkAlert acknowledgedAlert = alertRepository.findById(alertId).orElseThrow();
        assertThat(acknowledgedAlert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);

        // Probe 4 & 5: Repeated failures while alert is ACKNOWLEDGED must STILL suppress duplicate alerts
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk());

        List<NetworkAlert> alertsAfterAckProbes = alertRepository.findByDeviceId(testDevice.getId());
        assertThat(alertsAfterAckProbes).hasSize(1);
        assertThat(alertsAfterAckProbes.get(0).getId()).isEqualTo(alertId);
        assertThat(alertsAfterAckProbes.get(0).getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    @DisplayName("Subsequent REACHABLE probe auto-resolves active DEVICE_UNREACHABLE alert with recovery note")
    void testSubsequentReachable_AutoResolvesActiveUnreachableAlert() throws Exception {
        // Seed active UNREACHABLE alert
        NetworkAlert initialAlert = new NetworkAlert(
                testDevice,
                AlertType.DEVICE_UNREACHABLE,
                AlertSeverity.CRITICAL,
                AlertStatus.OPEN,
                "Device is unreachable via ICMP echo and management ports.",
                "REACHABILITY_MONITOR"
        );
        initialAlert = alertRepository.save(initialAlert);
        Long alertId = initialAlert.getId();

        // Stub recovery to REACHABLE via TCP port 22
        DeviceReachabilityResponseDto reachableResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                true,
                "REACHABLE",
                "TCP_CONNECTION",
                22,
                14L,
                Instant.now(),
                "Target device confirmed reachable via TCP connection on port 22 in 14ms."
        );

        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(reachableResponse);

        // Trigger reachability probe
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable", is(true)))
                .andExpect(jsonPath("$.reachabilityStatus", is("REACHABLE")));

        // Verify alert auto-resolved in H2
        NetworkAlert resolvedAlert = alertRepository.findById(alertId).orElseThrow();
        assertThat(resolvedAlert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(resolvedAlert.getResolvedAt()).isNotNull();
        assertThat(resolvedAlert.getMessage()).contains("[Auto-recovered: Device confirmed reachable via TCP_CONNECTION (14ms)]");

        // Verify no active alerts remain for this device
        List<NetworkAlert> activeAlerts = alertRepository.findByDeviceIdAndStatusIn(
                testDevice.getId(),
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        assertThat(activeAlerts).isEmpty();
    }

    @Test
    @DisplayName("INVALID_TARGET and INCONCLUSIVE probes do not create false alerts and do not resolve existing alerts")
    void testInvalidTargetAndInconclusive_DoNotCreateFalseAlertsOrResolveExistingAlerts() throws Exception {
        // Part 1: INVALID_TARGET does not generate alerts on clean device
        DeviceReachabilityResponseDto invalidTargetResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                false,
                "INVALID_TARGET",
                "NONE",
                null,
                null,
                Instant.now(),
                "Target IP address format is invalid: 'invalid-ip'."
        );

        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(invalidTargetResponse);

        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachabilityStatus", is("INVALID_TARGET")));

        assertThat(alertRepository.findByDeviceId(testDevice.getId())).isEmpty();

        // Part 2: INCONCLUSIVE does not generate alerts
        DeviceReachabilityResponseDto inconclusiveResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                false,
                "INCONCLUSIVE",
                "NONE",
                null,
                null,
                Instant.now(),
                "Reachability check inconclusive: Probe execution interrupted or socket error."
        );

        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(inconclusiveResponse);

        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachabilityStatus", is("INCONCLUSIVE")));

        assertThat(alertRepository.findByDeviceId(testDevice.getId())).isEmpty();

        // Part 3: Seed an active alert and verify INCONCLUSIVE / INVALID_TARGET do NOT resolve it
        NetworkAlert existingAlert = new NetworkAlert(
                testDevice,
                AlertType.DEVICE_UNREACHABLE,
                AlertSeverity.CRITICAL,
                AlertStatus.OPEN,
                "Device is unreachable via ICMP echo.",
                "REACHABILITY_MONITOR"
        );
        existingAlert = alertRepository.save(existingAlert);
        Long existingAlertId = existingAlert.getId();

        // Run INCONCLUSIVE probe
        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachabilityStatus", is("INCONCLUSIVE")));

        NetworkAlert afterInconclusive = alertRepository.findById(existingAlertId).orElseThrow();
        assertThat(afterInconclusive.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(afterInconclusive.getResolvedAt()).isNull();

        // Run INVALID_TARGET probe
        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(invalidTargetResponse);

        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachabilityStatus", is("INVALID_TARGET")));

        NetworkAlert afterInvalidTarget = alertRepository.findById(existingAlertId).orElseThrow();
        assertThat(afterInvalidTarget.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(afterInvalidTarget.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("REST Alert API reflects reachability alert creation and auto-recovery lifecycle")
    void testRestAlertApi_ReflectsCreationAndRecovery() throws Exception {
        // Initially no alerts for device
        mockMvc.perform(get("/api/alerts")
                        .param("deviceId", testDevice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 1. UNREACHABLE probe generates alert
        DeviceReachabilityResponseDto unreachableResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                false,
                "UNREACHABLE",
                "NONE",
                null,
                null,
                Instant.now(),
                "Target device did not respond to ICMP echo or TCP probes within 2000ms."
        );
        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(unreachableResponse);

        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk());

        // Verify GET /api/alerts returns created alert
        mockMvc.perform(get("/api/alerts")
                        .param("deviceId", testDevice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alertType", is("DEVICE_UNREACHABLE")))
                .andExpect(jsonPath("$[0].severity", is("CRITICAL")))
                .andExpect(jsonPath("$[0].status", is("OPEN")))
                .andExpect(jsonPath("$[0].source", is("REACHABILITY_MONITOR")))
                .andExpect(jsonPath("$[0].resolvedAt", nullValue()));

        List<NetworkAlert> alerts = alertRepository.findByDeviceId(testDevice.getId());
        Long alertId = alerts.get(0).getId();

        // Verify GET /api/alerts/{id}
        mockMvc.perform(get("/api/alerts/{id}", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(alertId.intValue())))
                .andExpect(jsonPath("$.status", is("OPEN")));

        // 2. REACHABLE probe auto-recovers device
        DeviceReachabilityResponseDto reachableResponse = new DeviceReachabilityResponseDto(
                testDevice.getId(),
                testDevice.getHostname(),
                testDevice.getManagementIp(),
                true,
                "REACHABLE",
                "TCP_CONNECTION",
                22,
                12L,
                Instant.now(),
                "Target device confirmed reachable via TCP connection on port 22 in 12ms."
        );
        when(reachabilityService.checkReachability(eq(testDevice.getId()), any(), any(), any()))
                .thenReturn(reachableResponse);

        mockMvc.perform(post("/api/devices/{id}/reachability", testDevice.getId()))
                .andExpect(status().isOk());

        // Verify GET /api/alerts reflects RESOLVED status
        mockMvc.perform(get("/api/alerts")
                        .param("deviceId", testDevice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(alertId.intValue())))
                .andExpect(jsonPath("$[0].status", is("RESOLVED")))
                .andExpect(jsonPath("$[0].resolvedAt", notNullValue()))
                .andExpect(jsonPath("$[0].message", containsString("Auto-recovered")));

        // Verify filtering by status
        mockMvc.perform(get("/api/alerts")
                        .param("deviceId", testDevice.getId().toString())
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/alerts")
                        .param("deviceId", testDevice.getId().toString())
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(alertId.intValue())));
    }
}
