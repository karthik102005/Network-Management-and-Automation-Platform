package com.nmap.controller;

import com.nmap.dto.TopologyResponseDto;
import com.nmap.service.TopologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topology")
public class TopologyController {

    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping
    public ResponseEntity<TopologyResponseDto> getTopology() {
        return ResponseEntity.ok(topologyService.getTopology());
    }
}
