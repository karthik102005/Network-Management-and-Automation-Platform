package com.nmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.AutomationChangeRequestCreateDto;
import com.nmap.dto.AutomationChangeRequestResponseDto;
import com.nmap.dto.AutomationExecutionStepDto;
import com.nmap.dto.AutomationPlaybookDto;
import com.nmap.entity.AutomationStatus;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.service.AutomationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AutomationController.class)
@Import(GlobalExceptionHandler.class)
class AutomationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutomationService automationService;

    private AutomationPlaybookDto samplePlaybook;
    private AutomationChangeRequestResponseDto sampleDraftDto;
    private AutomationChangeRequestResponseDto sampleCompletedDto;

    @BeforeEach
    void setUp() {
        samplePlaybook = new AutomationPlaybookDto(
                "playbook-vlan-provision",
                "VLAN & Subnet Provisioning",
                "SWITCHING",
                "CISCO",
                "Automates VLAN creation",
                List.of("VLAN_ID", "VLAN_NAME"),
                "vlan {{VLAN_ID}}\n name {{VLAN_NAME}}\nexit"
        );

        sampleDraftDto = new AutomationChangeRequestResponseDto(
                1L, 1L, "R1", "Provision VLAN 100", "Engineering VLAN",
                "playbook-vlan-provision", AutomationStatus.DRAFT,
                "vlan 100\n name Engineering\nexit", 3, null, null,
                "operator", List.of(), null, null, true,
                "Simulated automation execution. No live hardware commands issued.",
                Instant.now(), null, null
        );

        sampleCompletedDto = new AutomationChangeRequestResponseDto(
                1L, 1L, "R1", "Provision VLAN 100", "Engineering VLAN",
                "playbook-vlan-provision", AutomationStatus.COMPLETED,
                "vlan 100\n name Engineering\nexit", 3, 4, null,
                "operator",
                List.of(new AutomationExecutionStepDto(1, "SIMULATED_PRE_CHECK", "PASSED", "Verified", Instant.now())),
                "R1# configure terminal\n[OK]", null, true,
                "Simulated automation execution. No live hardware commands issued.",
                Instant.now(), Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/automation/playbooks should return 200 with list of playbooks")
    void testGetPlaybooks() throws Exception {
        when(automationService.getPlaybooks()).thenReturn(List.of(samplePlaybook));

        mockMvc.perform(get("/api/automation/playbooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("playbook-vlan-provision")))
                .andExpect(jsonPath("$[0].category", is("SWITCHING")));

        verify(automationService, times(1)).getPlaybooks();
    }

    @Test
    @DisplayName("GET /api/automation/requests should return 200 with change requests")
    void testGetAllChangeRequests() throws Exception {
        when(automationService.getAllChangeRequests()).thenReturn(List.of(sampleDraftDto));

        mockMvc.perform(get("/api/automation/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].status", is("DRAFT")))
                .andExpect(jsonPath("$[0].isSimulated", is(true)));

        verify(automationService, times(1)).getAllChangeRequests();
    }

    @Test
    @DisplayName("GET /api/automation/requests/{id} should return 200 with request details")
    void testGetChangeRequestById() throws Exception {
        when(automationService.getChangeRequestById(1L)).thenReturn(sampleCompletedDto);

        mockMvc.perform(get("/api/automation/requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.postChangeVersion", is(4)))
                .andExpect(jsonPath("$.terminalTranscript", containsString("configure terminal")));

        verify(automationService, times(1)).getChangeRequestById(1L);
    }

    @Test
    @DisplayName("GET /api/automation/requests/{id} should return 404 when not found")
    void testGetChangeRequestByIdNotFound() throws Exception {
        when(automationService.getChangeRequestById(99L))
                .thenThrow(new ResourceNotFoundException("AutomationChangeRequest", "id", 99L));

        mockMvc.perform(get("/api/automation/requests/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("AutomationChangeRequest not found with id: '99'")));

        verify(automationService, times(1)).getChangeRequestById(99L);
    }

    @Test
    @DisplayName("POST /api/automation/requests should return 201 Created with Location header")
    void testCreateChangeRequestSuccess() throws Exception {
        AutomationChangeRequestCreateDto createDto = new AutomationChangeRequestCreateDto(
                1L, "Provision VLAN 100", "Engineering VLAN",
                "playbook-vlan-provision", "vlan 100\n name Engineering\nexit", "operator"
        );

        when(automationService.createChangeRequest(any(AutomationChangeRequestCreateDto.class))).thenReturn(sampleDraftDto);

        mockMvc.perform(post("/api/automation/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/automation/requests/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        verify(automationService, times(1)).createChangeRequest(any(AutomationChangeRequestCreateDto.class));
    }

    @Test
    @DisplayName("POST /api/automation/requests should return 400 when title or commands are blank")
    void testCreateChangeRequestValidationFailure() throws Exception {
        AutomationChangeRequestCreateDto invalidDto = new AutomationChangeRequestCreateDto(
                1L, "", "Desc", "playbook", "", "operator"
        );

        mockMvc.perform(post("/api/automation/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.title", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.configCommands", notNullValue()));

        verify(automationService, never()).createChangeRequest(any());
    }

    @Test
    @DisplayName("POST /api/automation/requests/{id}/execute should return 200 with executed result")
    void testExecuteChangeRequestSuccess() throws Exception {
        when(automationService.executeChangeRequest(1L)).thenReturn(sampleCompletedDto);

        mockMvc.perform(post("/api/automation/requests/1/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.postChangeVersion", is(4)));

        verify(automationService, times(1)).executeChangeRequest(1L);
    }

    @Test
    @DisplayName("POST /api/automation/requests/{id}/execute should return 409 Conflict when stale configuration detected")
    void testExecuteChangeRequestConflict() throws Exception {
        when(automationService.executeChangeRequest(1L))
                .thenThrow(new IllegalStateException("Stale change request detected: The device active configuration has changed"));

        mockMvc.perform(post("/api/automation/requests/1/execute"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Stale change request detected")));

        verify(automationService, times(1)).executeChangeRequest(1L);
    }

    @Test
    @DisplayName("POST /api/automation/requests/{id}/rollback should return 200 with rolled back result")
    void testRollbackChangeRequestSuccess() throws Exception {
        AutomationChangeRequestResponseDto rolledBackDto = new AutomationChangeRequestResponseDto(
                1L, 1L, "R1", "Provision VLAN 100", "Engineering VLAN",
                "playbook-vlan-provision", AutomationStatus.ROLLED_BACK,
                "vlan 100\n name Engineering\nexit", 3, 4, 5,
                "operator", List.of(), "Rolled back", null, true,
                "Simulated automation execution. No live hardware commands issued.",
                Instant.now(), Instant.now(), Instant.now()
        );

        when(automationService.rollbackChangeRequest(1L)).thenReturn(rolledBackDto);

        mockMvc.perform(post("/api/automation/requests/1/rollback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("ROLLED_BACK")))
                .andExpect(jsonPath("$.rollbackVersion", is(5)));

        verify(automationService, times(1)).rollbackChangeRequest(1L);
    }
}
