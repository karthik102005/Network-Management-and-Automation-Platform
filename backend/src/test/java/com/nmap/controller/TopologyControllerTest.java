package com.nmap.controller;

import com.nmap.dto.TopologyResponseDto;
import com.nmap.dto.TopologyResponseDto.TopologyLinkDto;
import com.nmap.dto.TopologyResponseDto.TopologyNodeDto;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.LinkStatus;
import com.nmap.entity.LinkType;
import com.nmap.service.TopologyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopologyController.class)
class TopologyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopologyService topologyService;

    @Test
    @DisplayName("GET /api/topology should return nodes and links in graph structure")
    void testGetTopology() throws Exception {
        TopologyNodeDto node1 = new TopologyNodeDto(1L, "R1", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, 2L);
        TopologyNodeDto node2 = new TopologyNodeDto(2L, "SW1", "192.168.10.2", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.UP, 4L);

        TopologyLinkDto link1 = new TopologyLinkDto(
                100L, 1L, "R1", 10L, "Gi0/0",
                2L, "SW1", 20L, "Gi0/1",
                LinkStatus.UP, LinkType.ETHERNET, 1000L
        );

        TopologyResponseDto response = new TopologyResponseDto(List.of(node1, node2), List.of(link1));

        when(topologyService.getTopology()).thenReturn(response);

        mockMvc.perform(get("/api/topology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes", is(2)))
                .andExpect(jsonPath("$.totalLinks", is(1)))
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.nodes[0].hostname", is("R1")))
                .andExpect(jsonPath("$.links[0].sourceHostname", is("R1")))
                .andExpect(jsonPath("$.links[0].destinationHostname", is("SW1")));
    }
}
