package com.nmap.controller;

import com.nmap.dto.InterfaceRequestDto;
import com.nmap.dto.InterfaceResponseDto;
import com.nmap.service.NetworkInterfaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class NetworkInterfaceController {

    private final NetworkInterfaceService interfaceService;

    public NetworkInterfaceController(NetworkInterfaceService interfaceService) {
        this.interfaceService = interfaceService;
    }

    @GetMapping("/interfaces")
    public ResponseEntity<List<InterfaceResponseDto>> getAllInterfaces() {
        return ResponseEntity.ok(interfaceService.getAllInterfaces());
    }

    @GetMapping("/interfaces/{id}")
    public ResponseEntity<InterfaceResponseDto> getInterfaceById(@PathVariable Long id) {
        return ResponseEntity.ok(interfaceService.getInterfaceById(id));
    }

    @GetMapping("/devices/{deviceId}/interfaces")
    public ResponseEntity<List<InterfaceResponseDto>> getInterfacesByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(interfaceService.getInterfacesByDeviceId(deviceId));
    }

    @PostMapping("/interfaces")
    public ResponseEntity<InterfaceResponseDto> createInterface(@Valid @RequestBody InterfaceRequestDto requestDto) {
        InterfaceResponseDto created = interfaceService.createInterface(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/interfaces/{id}")
    public ResponseEntity<InterfaceResponseDto> updateInterface(
            @PathVariable Long id,
            @Valid @RequestBody InterfaceRequestDto requestDto) {
        return ResponseEntity.ok(interfaceService.updateInterface(id, requestDto));
    }

    @DeleteMapping("/interfaces/{id}")
    public ResponseEntity<Void> deleteInterface(@PathVariable Long id) {
        interfaceService.deleteInterface(id);
        return ResponseEntity.noContent().build();
    }
}
