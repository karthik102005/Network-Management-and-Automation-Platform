package com.nmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.dto.DeviceRequestDto;
import com.nmap.dto.DeviceResponseDto;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.service.NetworkDeviceService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkDeviceController.class)
@Import(GlobalExceptionHandler.class)
class NetworkDeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NetworkDeviceService deviceService;

    private DeviceResponseDto sampleResponse;
    private DeviceRequestDto validRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new DeviceResponseDto(
                1L,
                "edge-router01",
                "10.0.0.1",
                DeviceType.ROUTER,
                DeviceVendor.CISCO,
                DeviceStatus.UP,
                "Edge Border Gateway Router",
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        validRequest = new DeviceRequestDto(
                "edge-router01",
                "10.0.0.1",
                DeviceType.ROUTER,
                DeviceVendor.CISCO,
                DeviceStatus.UP,
                "Edge Border Gateway Router"
        );
    }

    @Test
    @DisplayName("GET /api/devices should return 200 with device list")
    void testGetAllDevices() throws Exception {
        when(deviceService.getAllDevices()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hostname", is("edge-router01")))
                .andExpect(jsonPath("$[0].managementIp", is("10.0.0.1")))
                .andExpect(jsonPath("$[0].deviceType", is("ROUTER")))
                .andExpect(jsonPath("$[0].vendor", is("CISCO")));

        verify(deviceService, times(1)).getAllDevices();
    }

    @Test
    @DisplayName("GET /api/devices/{id} should return 200 when device exists")
    void testGetDeviceByIdSuccess() throws Exception {
        when(deviceService.getDeviceById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/devices/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.hostname", is("edge-router01")))
                .andExpect(jsonPath("$.managementIp", is("10.0.0.1")));

        verify(deviceService, times(1)).getDeviceById(1L);
    }

    @Test
    @DisplayName("GET /api/devices/{id} should return 404 when device not found")
    void testGetDeviceByIdNotFound() throws Exception {
        when(deviceService.getDeviceById(99L))
                .thenThrow(new ResourceNotFoundException("NetworkDevice", "id", 99L));

        mockMvc.perform(get("/api/devices/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("NetworkDevice not found with id: '99'")));

        verify(deviceService, times(1)).getDeviceById(99L);
    }

    @Test
    @DisplayName("POST /api/devices with valid payload should return 201 Created")
    void testCreateDeviceSuccess() throws Exception {
        when(deviceService.createDevice(any(DeviceRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/devices/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.hostname", is("edge-router01")));

        verify(deviceService, times(1)).createDevice(any(DeviceRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/devices with invalid IP should return 400 Bad Request")
    void testCreateDeviceInvalidIp() throws Exception {
        DeviceRequestDto invalidRequest = new DeviceRequestDto(
                "edge-router01",
                "999.999.999.999", // Invalid IPv4
                DeviceType.ROUTER,
                DeviceVendor.CISCO,
                DeviceStatus.UP,
                "Invalid IP device"
        );

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.managementIp", notNullValue()));

        verify(deviceService, never()).createDevice(any(DeviceRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/devices with duplicate IP should return 409 Conflict")
    void testCreateDeviceDuplicateConflict() throws Exception {
        when(deviceService.createDevice(any(DeviceRequestDto.class)))
                .thenThrow(new DuplicateResourceException("NetworkDevice", "managementIp", "10.0.0.1"));

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already exists with managementIp: '10.0.0.1'")));
    }

    @Test
    @DisplayName("PUT /api/devices/{id} should return 200 with updated device")
    void testUpdateDeviceSuccess() throws Exception {
        when(deviceService.updateDevice(eq(1L), any(DeviceRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/devices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.hostname", is("edge-router01")));

        verify(deviceService, times(1)).updateDevice(eq(1L), any(DeviceRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/devices/{id} should return 204 No Content")
    void testDeleteDeviceSuccess() throws Exception {
        doNothing().when(deviceService).deleteDevice(1L);

        mockMvc.perform(delete("/api/devices/1"))
                .andExpect(status().isNoContent());

        verify(deviceService, times(1)).deleteDevice(1L);
    }

    @Test
    @DisplayName("POST /api/devices/{id}/reachability should return 200 with reachability report")
    void testCheckDeviceReachabilityPostSuccess() throws Exception {
        DeviceReachabilityResponseDto reachabilityResult = new DeviceReachabilityResponseDto(
                1L,
                "edge-router01",
                "10.0.0.1",
                true,
                "REACHABLE",
                "ICMP_ECHO",
                null,
                14L,
                Instant.now(),
                "Target device responded to ICMP echo probe in 14ms."
        );

        when(deviceService.checkDeviceReachability(1L, null)).thenReturn(reachabilityResult);

        mockMvc.perform(post("/api/devices/1/reachability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId", is(1)))
                .andExpect(jsonPath("$.hostname", is("edge-router01")))
                .andExpect(jsonPath("$.managementIp", is("10.0.0.1")))
                .andExpect(jsonPath("$.reachable", is(true)))
                .andExpect(jsonPath("$.reachabilityStatus", is("REACHABLE")))
                .andExpect(jsonPath("$.probeMethod", is("ICMP_ECHO")))
                .andExpect(jsonPath("$.responseTimeMs", is(14)))
                .andExpect(jsonPath("$.details", containsString("14ms")));

        verify(deviceService, times(1)).checkDeviceReachability(1L, null);
    }

    @Test
    @DisplayName("GET /api/devices/{id}/reachability should return 200 with reachability report")
    void testGetDeviceReachabilitySuccess() throws Exception {
        DeviceReachabilityResponseDto reachabilityResult = new DeviceReachabilityResponseDto(
                1L,
                "edge-router01",
                "10.0.0.1",
                true,
                "REACHABLE",
                "TCP_CONNECTION",
                22,
                25L,
                Instant.now(),
                "Target device confirmed reachable via TCP connection on port 22 in 25ms."
        );

        when(deviceService.checkDeviceReachability(1L, 1500)).thenReturn(reachabilityResult);

        mockMvc.perform(get("/api/devices/1/reachability?timeoutMs=1500")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId", is(1)))
                .andExpect(jsonPath("$.port", is(22)))
                .andExpect(jsonPath("$.probeMethod", is("TCP_CONNECTION")))
                .andExpect(jsonPath("$.responseTimeMs", is(25)));

        verify(deviceService, times(1)).checkDeviceReachability(1L, 1500);
    }

    @Test
    @DisplayName("POST /api/devices/{id}/reachability should return 404 when device not found")
    void testCheckDeviceReachabilityNotFound() throws Exception {
        when(deviceService.checkDeviceReachability(99L, null))
                .thenThrow(new ResourceNotFoundException("NetworkDevice", "id", 99L));

        mockMvc.perform(post("/api/devices/99/reachability"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("NetworkDevice not found with id: '99'")));

        verify(deviceService, times(1)).checkDeviceReachability(99L, null);
    }

    @Test
    @DisplayName("POST /api/devices/{id}/reachability should return 200 with UNREACHABLE details when host down")
    void testCheckDeviceReachabilityUnreachable() throws Exception {
        DeviceReachabilityResponseDto unreachableResult = new DeviceReachabilityResponseDto(
                1L,
                "edge-router01",
                "10.0.0.1",
                false,
                "UNREACHABLE",
                "NONE",
                null,
                null,
                Instant.now(),
                "Target device did not respond to ICMP echo or TCP probes within 2000ms."
        );

        when(deviceService.checkDeviceReachability(1L, null)).thenReturn(unreachableResult);

        mockMvc.perform(post("/api/devices/1/reachability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable", is(false)))
                .andExpect(jsonPath("$.reachabilityStatus", is("UNREACHABLE")))
                .andExpect(jsonPath("$.responseTimeMs").doesNotExist());
    }
}
