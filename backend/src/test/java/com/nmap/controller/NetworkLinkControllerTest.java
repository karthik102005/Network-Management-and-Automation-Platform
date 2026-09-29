package com.nmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmap.dto.LinkRequestDto;
import com.nmap.dto.LinkResponseDto;
import com.nmap.entity.LinkStatus;
import com.nmap.entity.LinkType;
import com.nmap.exception.GlobalExceptionHandler;
import com.nmap.service.NetworkLinkService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkLinkController.class)
@Import(GlobalExceptionHandler.class)
class NetworkLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NetworkLinkService linkService;

    private LinkResponseDto sampleResponse;
    private LinkRequestDto validRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new LinkResponseDto(
                100L, 10L, "Gi0/0", 1L, "R1",
                20L, "Gi0/1", 2L, "SW1",
                LinkStatus.UP, LinkType.ETHERNET, 1000L,
                "Trunk Link", Instant.now(), Instant.now()
        );

        validRequest = new LinkRequestDto(
                10L, 20L, LinkStatus.UP, LinkType.ETHERNET, 1000L, "Trunk Link"
        );
    }

    @Test
    @DisplayName("GET /api/links should return 200 with links list")
    void testGetAllLinks() throws Exception {
        when(linkService.getAllLinks()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceHostname", is("R1")))
                .andExpect(jsonPath("$[0].destinationHostname", is("SW1")));
    }

    @Test
    @DisplayName("POST /api/links with valid payload should return 201 Created")
    void testCreateLinkSuccess() throws Exception {
        when(linkService.createLink(any(LinkRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(100)));
    }

    @Test
    @DisplayName("DELETE /api/links/{id} should return 204 No Content")
    void testDeleteLinkSuccess() throws Exception {
        doNothing().when(linkService).deleteLink(100L);

        mockMvc.perform(delete("/api/links/100"))
                .andExpect(status().isNoContent());
    }
}
