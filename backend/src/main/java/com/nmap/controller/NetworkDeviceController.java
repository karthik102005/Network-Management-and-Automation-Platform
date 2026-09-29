package com.nmap.controller;

import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.dto.DeviceRequestDto;
import com.nmap.dto.DeviceResponseDto;
import com.nmap.service.NetworkDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class NetworkDeviceController {

    private final NetworkDeviceService deviceService;

    public NetworkDeviceController(NetworkDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponseDto>> getAllDevices() {
        List<DeviceResponseDto> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDeviceById(@PathVariable Long id) {
        DeviceResponseDto device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(device);
    }

    @PostMapping
    public ResponseEntity<DeviceResponseDto> createDevice(@Valid @RequestBody DeviceRequestDto requestDto) {
        DeviceResponseDto createdDevice = deviceService.createDevice(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdDevice.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdDevice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequestDto requestDto) {
        DeviceResponseDto updatedDevice = deviceService.updateDevice(id, requestDto);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reachability")
    public ResponseEntity<DeviceReachabilityResponseDto> checkDeviceReachability(
            @PathVariable Long id,
            @RequestParam(value = "timeoutMs", required = false) Integer timeoutMs) {
        DeviceReachabilityResponseDto response = deviceService.checkDeviceReachability(id, timeoutMs);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reachability")
    public ResponseEntity<DeviceReachabilityResponseDto> getDeviceReachability(
            @PathVariable Long id,
            @RequestParam(value = "timeoutMs", required = false) Integer timeoutMs) {
        DeviceReachabilityResponseDto response = deviceService.checkDeviceReachability(id, timeoutMs);
        return ResponseEntity.ok(response);
    }
}
