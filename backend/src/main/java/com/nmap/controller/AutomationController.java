package com.nmap.controller;

import com.nmap.dto.AutomationChangeRequestCreateDto;
import com.nmap.dto.AutomationChangeRequestResponseDto;
import com.nmap.dto.AutomationPlaybookDto;
import com.nmap.service.AutomationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/automation")
public class AutomationController {

    private final AutomationService automationService;

    public AutomationController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @GetMapping("/playbooks")
    public ResponseEntity<List<AutomationPlaybookDto>> getPlaybooks() {
        return ResponseEntity.ok(automationService.getPlaybooks());
    }

    @GetMapping("/requests")
    public ResponseEntity<List<AutomationChangeRequestResponseDto>> getAllChangeRequests(
            @RequestParam(value = "deviceId", required = false) Long deviceId) {
        if (deviceId != null) {
            return ResponseEntity.ok(automationService.getChangeRequestsByDeviceId(deviceId));
        }
        return ResponseEntity.ok(automationService.getAllChangeRequests());
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<AutomationChangeRequestResponseDto> getChangeRequestById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(automationService.getChangeRequestById(id));
    }

    @PostMapping("/requests")
    public ResponseEntity<AutomationChangeRequestResponseDto> createChangeRequest(
            @Valid @RequestBody AutomationChangeRequestCreateDto createDto) {
        AutomationChangeRequestResponseDto created = automationService.createChangeRequest(createDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/requests/{id}/execute")
    public ResponseEntity<AutomationChangeRequestResponseDto> executeChangeRequest(@PathVariable("id") Long id) {
        AutomationChangeRequestResponseDto executed = automationService.executeChangeRequest(id);
        return ResponseEntity.ok(executed);
    }

    @PostMapping("/requests/{id}/rollback")
    public ResponseEntity<AutomationChangeRequestResponseDto> rollbackChangeRequest(@PathVariable("id") Long id) {
        AutomationChangeRequestResponseDto rolledBack = automationService.rollbackChangeRequest(id);
        return ResponseEntity.ok(rolledBack);
    }
}
