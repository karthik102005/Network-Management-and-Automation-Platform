package com.nmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.InterfaceRequestDto;
import com.nmap.dto.InterfaceResponseDto;
import com.nmap.entity.AdminStatus;
import com.nmap.entity.InterfaceType;
import com.nmap.entity.OperationalStatus;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.service.NetworkInterfaceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkInterfaceController.class)
@Import(GlobalExceptionHandler.class)
class NetworkInterfaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NetworkInterfaceService interfaceService;

    private InterfaceResponseDto sampleResponse;
    private InterfaceRequestDto validRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new InterfaceResponseDto(
                10L, 1L, "core-r1", "GigabitEthernet0/0/0",
                InterfaceType.GIGABIT_ETHERNET, "10.0.0.1", 24,
                "00:1A:2B:3C:4D:5E", AdminStatus.UP, OperationalStatus.UP,
                1000L, "Uplink", Instant.now(), Instant.now()
        );

        validRequest = new InterfaceRequestDto(
                1L, "GigabitEthernet0/0/0", InterfaceType.GIGABIT_ETHERNET,
                "10.0.0.1", 24, "00:1A:2B:3C:4D:5E", AdminStatus.UP,
                OperationalStatus.UP, 1000L, "Uplink"
        );
    }

    @Test
    @DisplayName("GET /api/interfaces should return 200 with interface list")
    void testGetAllInterfaces() throws Exception {
        when(interfaceService.getAllInterfaces()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/interfaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].interfaceName", is("GigabitEthernet0/0/0")));
    }

    @Test
    @DisplayName("GET /api/devices/{deviceId}/interfaces should return interfaces for that device")
    void testGetInterfacesByDeviceId() throws Exception {
        when(interfaceService.getInterfacesByDeviceId(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/devices/1/interfaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId", is(1)));
    }

    @Test
    @DisplayName("POST /api/interfaces with valid payload should return 201 Created")
    void testCreateInterfaceSuccess() throws Exception {
        when(interfaceService.createInterface(any(InterfaceRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/interfaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.interfaceName", is("GigabitEthernet0/0/0")));
    }

    @Test
    @DisplayName("POST /api/interfaces with invalid IP should return 400 Bad Request")
    void testCreateInterfaceInvalidIp() throws Exception {
        InterfaceRequestDto invalid = new InterfaceRequestDto(
                1L, "eth0", InterfaceType.ETHERNET, "999.999.999.999", 24, null, AdminStatus.UP, OperationalStatus.UP, 1000L, null
        );

        mockMvc.perform(post("/api/interfaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.ipAddress", notNullValue()));
    }

    @Test
    @DisplayName("DELETE /api/interfaces/{id} should return 204 No Content")
    void testDeleteInterface() throws Exception {
        doNothing().when(interfaceService).deleteInterface(10L);

        mockMvc.perform(delete("/api/interfaces/10"))
                .andExpect(status().isNoContent());
    }
}
