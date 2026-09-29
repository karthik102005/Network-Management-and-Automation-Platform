package com.nmap.controller;

import com.nmap.dto.ConfigurationDiffResponseDto;
import com.nmap.dto.DeviceConfigurationRequestDto;
import com.nmap.dto.DeviceConfigurationResponseDto;
import com.nmap.service.DeviceConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/devices/{id}/configurations")
public class DeviceConfigurationController {

    private final DeviceConfigurationService configurationService;

    public DeviceConfigurationController(DeviceConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceConfigurationResponseDto>> getConfigurations(@PathVariable("id") Long id) {
        List<DeviceConfigurationResponseDto> configs = configurationService.getConfigurationsByDeviceId(id);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/diff")
    public ResponseEntity<ConfigurationDiffResponseDto> compareConfigurations(
            @PathVariable("id") Long id,
            @RequestParam("v1") Integer v1,
            @RequestParam("v2") Integer v2) {
        if (v1 == null || v2 == null || v1 <= 0 || v2 <= 0) {
            throw new IllegalArgumentException("Version parameters v1 and v2 must be positive integers");
        }
        ConfigurationDiffResponseDto diff = configurationService.compareConfigurations(id, v1, v2);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/{version:\\d+}")
    public ResponseEntity<DeviceConfigurationResponseDto> getConfigurationByVersion(
            @PathVariable("id") Long id,
            @PathVariable("version") Integer version) {
        DeviceConfigurationResponseDto config = configurationService.getConfigurationByVersion(id, version);
        return ResponseEntity.ok(config);
    }

    @PostMapping
    public ResponseEntity<DeviceConfigurationResponseDto> createConfiguration(
            @PathVariable("id") Long id,
            @Valid @RequestBody DeviceConfigurationRequestDto requestDto) {
        DeviceConfigurationResponseDto created = configurationService.createConfiguration(id, requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{version}")
                .buildAndExpand(created.getVersion())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{version:\\d+}/restore")
    public ResponseEntity<DeviceConfigurationResponseDto> restoreConfiguration(
            @PathVariable("id") Long id,
            @PathVariable("version") Integer version) {
        DeviceConfigurationResponseDto restored = configurationService.restoreConfiguration(id, version);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/devices/{id}/configurations/{version}")
                .buildAndExpand(id, restored.getVersion())
                .toUri();
        return ResponseEntity.created(location).body(restored);
    }
}
