package com.nmap.service;

import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.dto.DeviceRequestDto;
import com.nmap.dto.DeviceResponseDto;

import java.util.List;

public interface NetworkDeviceService {

    List<DeviceResponseDto> getAllDevices();

    DeviceResponseDto getDeviceById(Long id);

    DeviceResponseDto createDevice(DeviceRequestDto requestDto);

    DeviceResponseDto updateDevice(Long id, DeviceRequestDto requestDto);

    void deleteDevice(Long id);

    DeviceReachabilityResponseDto checkDeviceReachability(Long id, Integer timeoutMs);
}
