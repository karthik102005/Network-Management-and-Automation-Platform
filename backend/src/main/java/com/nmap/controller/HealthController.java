package com.nmap.controller;

import com.nmap.dto.HealthResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final String applicationName;
    private final long startupTime;

    public HealthController(@Value("${spring.application.name:NetworkManagementApplication}") String applicationName) {
        this.applicationName = applicationName;
        this.startupTime = System.currentTimeMillis();
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, Object> details = new HashMap<>();
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("javaVendor", System.getProperty("java.vendor"));
        details.put("osName", System.getProperty("os.name"));
        details.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        details.put("freeMemoryBytes", Runtime.getRuntime().freeMemory());
        details.put("totalMemoryBytes", Runtime.getRuntime().totalMemory());

        HealthResponseDto response = new HealthResponseDto(
                "UP",
                applicationName,
                "1.0.0-SNAPSHOT",
                Instant.now(),
                uptime,
                details
        );

        return ResponseEntity.ok(response);
    }
}
