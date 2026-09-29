package com.nmap.service;

import com.nmap.dto.AutomationChangeRequestCreateDto;
import com.nmap.dto.AutomationChangeRequestResponseDto;
import com.nmap.dto.AutomationPlaybookDto;

import java.util.List;

public interface AutomationService {

    List<AutomationPlaybookDto> getPlaybooks();

    List<AutomationChangeRequestResponseDto> getAllChangeRequests();

    List<AutomationChangeRequestResponseDto> getChangeRequestsByDeviceId(Long deviceId);

    AutomationChangeRequestResponseDto getChangeRequestById(Long id);

    AutomationChangeRequestResponseDto createChangeRequest(AutomationChangeRequestCreateDto createDto);

    AutomationChangeRequestResponseDto executeChangeRequest(Long id);

    AutomationChangeRequestResponseDto rollbackChangeRequest(Long id);
}
