package com.nmap.service;

import com.nmap.dto.ConfigurationDiffResponseDto;
import com.nmap.dto.ConfigurationTemplateDto;
import com.nmap.dto.DeviceConfigurationRequestDto;
import com.nmap.dto.DeviceConfigurationResponseDto;

import java.util.List;

public interface DeviceConfigurationService {

    List<DeviceConfigurationResponseDto> getConfigurationsByDeviceId(Long deviceId);

    DeviceConfigurationResponseDto getConfigurationByVersion(Long deviceId, Integer version);

    DeviceConfigurationResponseDto createConfiguration(Long deviceId, DeviceConfigurationRequestDto requestDto);

    DeviceConfigurationResponseDto restoreConfiguration(Long deviceId, Integer versionToRestore);

    ConfigurationDiffResponseDto compareConfigurations(Long deviceId, Integer v1, Integer v2);

    List<ConfigurationTemplateDto> getTemplates();

    void deleteConfigurationsByDeviceId(Long deviceId);
}
