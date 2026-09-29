package com.nmap.controller;

import com.nmap.dto.ConfigurationTemplateDto;
import com.nmap.service.DeviceConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/configurations")
public class ConfigurationTemplateController {

    private final DeviceConfigurationService configurationService;

    public ConfigurationTemplateController(DeviceConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/templates")
    public ResponseEntity<List<ConfigurationTemplateDto>> getTemplates() {
        List<ConfigurationTemplateDto> templates = configurationService.getTemplates();
        return ResponseEntity.ok(templates);
    }
}
