package com.nmap.controller;

import com.nmap.dto.LinkRequestDto;
import com.nmap.dto.LinkResponseDto;
import com.nmap.service.NetworkLinkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/links")
public class NetworkLinkController {

    private final NetworkLinkService linkService;

    public NetworkLinkController(NetworkLinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping
    public ResponseEntity<List<LinkResponseDto>> getAllLinks() {
        return ResponseEntity.ok(linkService.getAllLinks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LinkResponseDto> getLinkById(@PathVariable Long id) {
        return ResponseEntity.ok(linkService.getLinkById(id));
    }

    @PostMapping
    public ResponseEntity<LinkResponseDto> createLink(@Valid @RequestBody LinkRequestDto requestDto) {
        LinkResponseDto created = linkService.createLink(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LinkResponseDto> updateLink(
            @PathVariable Long id,
            @Valid @RequestBody LinkRequestDto requestDto) {
        return ResponseEntity.ok(linkService.updateLink(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id) {
        linkService.deleteLink(id);
        return ResponseEntity.noContent().build();
    }
}
