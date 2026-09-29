package com.nmap.controller;

import com.nmap.dto.DeviceMetricsDto;
import com.nmap.dto.DeviceMetricsHistoryPointDto;
import com.nmap.service.DeviceMetricsProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices/{id}/metrics")
public class DeviceMetricsController {

    private final DeviceMetricsProvider metricsProvider;

    public DeviceMetricsController(DeviceMetricsProvider metricsProvider) {
        this.metricsProvider = metricsProvider;
    }

    @GetMapping
    public ResponseEntity<DeviceMetricsDto> getDeviceMetrics(@PathVariable("id") Long id) {
        DeviceMetricsDto metrics = metricsProvider.getDeviceMetrics(id);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DeviceMetricsHistoryPointDto>> getDeviceMetricsHistory(
            @PathVariable("id") Long id,
            @RequestParam(value = "points", defaultValue = "20") int points) {
        List<DeviceMetricsHistoryPointDto> history = metricsProvider.getDeviceMetricsHistory(id, points);
        return ResponseEntity.ok(history);
    }
}
