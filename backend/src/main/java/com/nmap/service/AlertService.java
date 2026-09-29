package com.nmap.service;

import com.nmap.dto.AlertResponseDto;
import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.entity.AlertSeverity;
import com.nmap.entity.AlertStatus;
import com.nmap.entity.NetworkDevice;

import java.util.List;

public interface AlertService {

    List<AlertResponseDto> getAlerts(AlertStatus status, AlertSeverity severity, Long deviceId);

    AlertResponseDto getAlertById(Long id);

    AlertResponseDto acknowledgeAlert(Long id);

    AlertResponseDto resolveAlert(Long id);

    void processReachabilityResult(NetworkDevice device, DeviceReachabilityResponseDto result);

    void deleteAlertsByDeviceId(Long deviceId);
}
