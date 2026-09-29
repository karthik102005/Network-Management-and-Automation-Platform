package com.nmap.controller;

import com.nmap.dto.AlertResponseDto;
import com.nmap.entity.AlertSeverity;
import com.nmap.entity.AlertStatus;
import com.nmap.entity.AlertType;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@Import(GlobalExceptionHandler.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    private AlertResponseDto sampleAlert;

    @BeforeEach
    void setUp() {
        sampleAlert = new AlertResponseDto(
                1L,
                10L,
                "core-rtr01",
                "192.168.10.1",
                AlertType.DEVICE_UNREACHABLE,
                AlertSeverity.CRITICAL,
                AlertStatus.OPEN,
                "Target host unreachable via ICMP and management ports",
                "REACHABILITY_MONITOR",
                Instant.now(),
                null,
                null
        );
    }

    @Test
    @DisplayName("GET /api/alerts should return 200 with list of alerts")
    void testGetAlerts() throws Exception {
        when(alertService.getAlerts(null, null, null)).thenReturn(List.of(sampleAlert));

        mockMvc.perform(get("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].deviceHostname", is("core-rtr01")))
                .andExpect(jsonPath("$[0].severity", is("CRITICAL")))
                .andExpect(jsonPath("$[0].status", is("OPEN")));

        verify(alertService, times(1)).getAlerts(null, null, null);
    }

    @Test
    @DisplayName("GET /api/alerts/{id} should return 200 when alert exists")
    void testGetAlertByIdSuccess() throws Exception {
        when(alertService.getAlertById(1L)).thenReturn(sampleAlert);

        mockMvc.perform(get("/api/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.alertType", is("DEVICE_UNREACHABLE")));

        verify(alertService, times(1)).getAlertById(1L);
    }

    @Test
    @DisplayName("GET /api/alerts/{id} should return 404 when alert does not exist")
    void testGetAlertByIdNotFound() throws Exception {
        when(alertService.getAlertById(99L))
                .thenThrow(new ResourceNotFoundException("NetworkAlert", "id", 99L));

        mockMvc.perform(get("/api/alerts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("NetworkAlert not found with id: '99'")));
    }

    @Test
    @DisplayName("PATCH /api/alerts/{id}/acknowledge should return 200 with ACKNOWLEDGED status")
    void testAcknowledgeAlertSuccess() throws Exception {
        sampleAlert.setStatus(AlertStatus.ACKNOWLEDGED);
        sampleAlert.setAcknowledgedAt(Instant.now());
        when(alertService.acknowledgeAlert(1L)).thenReturn(sampleAlert);

        mockMvc.perform(patch("/api/alerts/1/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACKNOWLEDGED")))
                .andExpect(jsonPath("$.acknowledgedAt", notNullValue()));

        verify(alertService, times(1)).acknowledgeAlert(1L);
    }

    @Test
    @DisplayName("PATCH /api/alerts/{id}/acknowledge should return 400 when already resolved")
    void testAcknowledgeAlreadyResolvedAlert() throws Exception {
        when(alertService.acknowledgeAlert(1L))
                .thenThrow(new IllegalArgumentException("Cannot acknowledge an already resolved alert"));

        mockMvc.perform(patch("/api/alerts/1/acknowledge"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Cannot acknowledge an already resolved alert")));
    }

    @Test
    @DisplayName("PATCH /api/alerts/{id}/resolve should return 200 with RESOLVED status")
    void testResolveAlertSuccess() throws Exception {
        sampleAlert.setStatus(AlertStatus.RESOLVED);
        sampleAlert.setResolvedAt(Instant.now());
        when(alertService.resolveAlert(1L)).thenReturn(sampleAlert);

        mockMvc.perform(patch("/api/alerts/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        verify(alertService, times(1)).resolveAlert(1L);
    }
}
