package com.nmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.*;
import com.nmap.entity.ConfigFormat;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.service.DeviceConfigurationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DeviceConfigurationController.class, ConfigurationTemplateController.class})
@Import(GlobalExceptionHandler.class)
class DeviceConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceConfigurationService configurationService;

    private DeviceConfigurationResponseDto sampleConfigV1;
    private DeviceConfigurationResponseDto sampleConfigV2;

    @BeforeEach
    void setUp() {
        sampleConfigV1 = new DeviceConfigurationResponseDto(
                10L,
                1L,
                "core-rtr01",
                1,
                "hostname core-rtr01\ninterface GigabitEthernet0/0/0",
                ConfigFormat.CISCO_IOS,
                "sha256hash1",
                false,
                "admin",
                "Initial config",
                Instant.now().minusSeconds(3600)
        );

        sampleConfigV2 = new DeviceConfigurationResponseDto(
                20L,
                1L,
                "core-rtr01",
                2,
                "hostname core-rtr01\ninterface GigabitEthernet0/0/0\ninterface Loopback0",
                ConfigFormat.CISCO_IOS,
                "sha256hash2",
                true,
                "admin",
                "Added Loopback",
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/devices/{id}/configurations should return 200 with list of configurations")
    void testGetConfigurations() throws Exception {
        when(configurationService.getConfigurationsByDeviceId(1L)).thenReturn(List.of(sampleConfigV2, sampleConfigV1));

        mockMvc.perform(get("/api/devices/1/configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].version", is(2)))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[1].version", is(1)))
                .andExpect(jsonPath("$[1].active", is(false)));

        verify(configurationService, times(1)).getConfigurationsByDeviceId(1L);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/configurations/{version} should return 200 with configuration details")
    void testGetConfigurationByVersion() throws Exception {
        when(configurationService.getConfigurationByVersion(1L, 1)).thenReturn(sampleConfigV1);

        mockMvc.perform(get("/api/devices/1/configurations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.configFormat", is("CISCO_IOS")))
                .andExpect(jsonPath("$.configText", containsString("hostname core-rtr01")));

        verify(configurationService, times(1)).getConfigurationByVersion(1L, 1);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/configurations/{version} should return 404 when not found")
    void testGetConfigurationByVersionNotFound() throws Exception {
        when(configurationService.getConfigurationByVersion(1L, 99))
                .thenThrow(new ResourceNotFoundException("DeviceConfiguration", "version", 99));

        mockMvc.perform(get("/api/devices/1/configurations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("DeviceConfiguration not found with version: '99'")));
    }

    @Test
    @DisplayName("POST /api/devices/{id}/configurations should return 201 Created with Location header")
    void testCreateConfigurationSuccess() throws Exception {
        DeviceConfigurationRequestDto request = new DeviceConfigurationRequestDto(
                "hostname core-rtr01\ninterface Loopback0",
                ConfigFormat.CISCO_IOS,
                "operator",
                "New snapshot"
        );

        when(configurationService.createConfiguration(eq(1L), any(DeviceConfigurationRequestDto.class)))
                .thenReturn(sampleConfigV2);

        mockMvc.perform(post("/api/devices/1/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/devices/1/configurations/2")))
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(jsonPath("$.active", is(true)));

        verify(configurationService, times(1)).createConfiguration(eq(1L), any(DeviceConfigurationRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/devices/{id}/configurations should return 400 when configText is blank")
    void testCreateConfigurationValidationFailure() throws Exception {
        DeviceConfigurationRequestDto request = new DeviceConfigurationRequestDto(
                "",
                ConfigFormat.CISCO_IOS,
                "operator",
                "New snapshot"
        );

        mockMvc.perform(post("/api/devices/1/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.configText", notNullValue()));

        verify(configurationService, never()).createConfiguration(any(), any());
    }

    @Test
    @DisplayName("POST /api/devices/{id}/configurations/{version}/restore should return 201 with new version")
    void testRestoreConfigurationSuccess() throws Exception {
        DeviceConfigurationResponseDto restoredV3 = new DeviceConfigurationResponseDto(
                30L, 1L, "core-rtr01", 3, sampleConfigV1.getConfigText(), ConfigFormat.CISCO_IOS,
                sampleConfigV1.getChecksum(), true, "operator (restored)", "Restored from version 1", Instant.now()
        );

        when(configurationService.restoreConfiguration(1L, 1)).thenReturn(restoredV3);

        mockMvc.perform(post("/api/devices/1/configurations/1/restore"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/devices/1/configurations/3")))
                .andExpect(jsonPath("$.version", is(3)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.description", containsString("Restored from version 1")));

        verify(configurationService, times(1)).restoreConfiguration(1L, 1);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/configurations/diff should return 200 with diff response and not conflict with {version}")
    void testCompareConfigurationsDiff() throws Exception {
        ConfigurationDiffResponseDto diffResponse = new ConfigurationDiffResponseDto(
                1L, "core-rtr01", 1, 2, false, 2, 3, 1, 0, 2,
                List.of(
                        new ConfigurationDiffLineDto(DiffLineType.UNCHANGED, 1, 1, "hostname core-rtr01"),
                        new ConfigurationDiffLineDto(DiffLineType.UNCHANGED, 2, 2, "interface GigabitEthernet0/0/0"),
                        new ConfigurationDiffLineDto(DiffLineType.ADDED, null, 3, "interface Loopback0")
                )
        );

        when(configurationService.compareConfigurations(1L, 1, 2)).thenReturn(diffResponse);

        mockMvc.perform(get("/api/devices/1/configurations/diff?v1=1&v2=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identical", is(false)))
                .andExpect(jsonPath("$.v1", is(1)))
                .andExpect(jsonPath("$.v2", is(2)))
                .andExpect(jsonPath("$.diffLines", hasSize(3)))
                .andExpect(jsonPath("$.diffLines[2].type", is("ADDED")));

        verify(configurationService, times(1)).compareConfigurations(1L, 1, 2);
    }

    @Test
    @DisplayName("GET /api/configurations/templates should return 200 with template list")
    void testGetTemplates() throws Exception {
        ConfigurationTemplateDto template = new ConfigurationTemplateDto(
                "cisco-ios-router-baseline",
                "Cisco IOS Router Baseline (Example)",
                "CISCO",
                "ROUTER",
                ConfigFormat.CISCO_IOS,
                "Description",
                "template text",
                "Example warning",
                List.of("HOSTNAME")
        );

        when(configurationService.getTemplates()).thenReturn(List.of(template));

        mockMvc.perform(get("/api/configurations/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("cisco-ios-router-baseline")))
                .andExpect(jsonPath("$[0].exampleWarning", is("Example warning")));

        verify(configurationService, times(1)).getTemplates();
    }
}
