package com.nmap.service;

import com.nmap.dto.InterfaceRequestDto;
import com.nmap.dto.InterfaceResponseDto;

import java.util.List;

public interface NetworkInterfaceService {

    List<InterfaceResponseDto> getAllInterfaces();

    InterfaceResponseDto getInterfaceById(Long id);

    List<InterfaceResponseDto> getInterfacesByDeviceId(Long deviceId);

    InterfaceResponseDto createInterface(InterfaceRequestDto requestDto);

    InterfaceResponseDto updateInterface(Long id, InterfaceRequestDto requestDto);

    void deleteInterface(Long id);
}
