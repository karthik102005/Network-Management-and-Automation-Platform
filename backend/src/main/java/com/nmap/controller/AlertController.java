package com.nmap.controller;

import com.nmap.dto.AlertResponseDto;
import com.nmap.entity.AlertSeverity;
import com.nmap.entity.AlertStatus;
import com.nmap.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<AlertResponseDto>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Long deviceId) {
        List<AlertResponseDto> alerts = alertService.getAlerts(status, severity, deviceId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponseDto> getAlertById(@PathVariable Long id) {
        AlertResponseDto alert = alertService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponseDto> acknowledgeAlert(@PathVariable Long id) {
        AlertResponseDto acknowledged = alertService.acknowledgeAlert(id);
        return ResponseEntity.ok(acknowledged);
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<AlertResponseDto> resolveAlert(@PathVariable Long id) {
        AlertResponseDto resolved = alertService.resolveAlert(id);
        return ResponseEntity.ok(resolved);
    }
}
