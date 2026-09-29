package com.nmap.exception;

import com.nmap.controller.AlertController;
import com.nmap.controller.DeviceConfigurationController;
import com.nmap.controller.NetworkDeviceController;
import com.nmap.service.AlertService;
import com.nmap.service.DeviceConfigurationService;
import com.nmap.service.NetworkDeviceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8.4.1: Unit and integration tests for GlobalExceptionHandler verifying
 * HTTP 400 Bad Request mapping on MethodArgumentTypeMismatchException and
 * regression checks for 404, 409, and 500 mappings.
 */
@WebMvcTest({NetworkDeviceController.class, AlertController.class, DeviceConfigurationController.class})
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NetworkDeviceService deviceService;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private DeviceConfigurationService configurationService;

    @Test
    @DisplayName("Non-numeric path variable ID should return HTTP 400 Bad Request with descriptive message")
    void testNonNumericDeviceId_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/devices/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid parameter value 'not-a-number' for parameter 'id'")))
                .andExpect(jsonPath("$.path", is("/api/devices/not-a-number")));
    }

    @Test
    @DisplayName("Invalid alert status enum query parameter should return HTTP 400 Bad Request")
    void testInvalidAlertStatusEnum_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/alerts?status=INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid parameter value 'INVALID_STATUS' for parameter 'status'")))
                .andExpect(jsonPath("$.path", is("/api/alerts")));
    }

    @Test
    @DisplayName("Malformed diff query parameter should return HTTP 400 Bad Request")
    void testMalformedConfigurationDiffVersion_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/devices/1/configurations/diff?v1=xyz&v2=4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid parameter value 'xyz' for parameter 'v1'")))
                .andExpect(jsonPath("$.path", is("/api/devices/1/configurations/diff")));
    }

    @Test
    @DisplayName("ResourceNotFoundException should return HTTP 404 Not Found")
    void testResourceNotFound_Regression_Returns404() throws Exception {
        when(deviceService.getDeviceById(99L))
                .thenThrow(new ResourceNotFoundException("NetworkDevice", "id", 99L));

        mockMvc.perform(get("/api/devices/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("NetworkDevice not found with id: '99'")))
                .andExpect(jsonPath("$.path", is("/api/devices/99")));
    }

    @Test
    @DisplayName("DuplicateResourceException should return HTTP 409 Conflict")
    void testDuplicateResource_Regression_Returns409() throws Exception {
        when(deviceService.getDeviceById(1L))
                .thenThrow(new DuplicateResourceException("NetworkDevice", "hostname", "R1"));

        mockMvc.perform(get("/api/devices/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("NetworkDevice already exists with hostname: 'R1'")));
    }

    @Test
    @DisplayName("IllegalStateException should return HTTP 409 Conflict")
    void testIllegalState_Regression_Returns409() throws Exception {
        when(deviceService.getDeviceById(1L))
                .thenThrow(new IllegalStateException("Active configuration mismatch conflict"));

        mockMvc.perform(get("/api/devices/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Active configuration mismatch conflict")));
    }

    @Test
    @DisplayName("Unhandled Exception should return HTTP 500 Internal Server Error without leaking internal stack")
    void testUnhandledException_Regression_Returns500() throws Exception {
        when(deviceService.getDeviceById(1L))
                .thenThrow(new RuntimeException("Database connection timeout"));

        mockMvc.perform(get("/api/devices/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected internal error occurred. Please contact the network administrator.")));
    }
}
