package com.nmap.service;

import com.nmap.dto.DeviceMetricsDto;
import com.nmap.dto.DeviceMetricsHistoryPointDto;

import java.util.List;

public interface DeviceMetricsProvider {

    DeviceMetricsDto getDeviceMetrics(Long deviceId);

    List<DeviceMetricsHistoryPointDto> getDeviceMetricsHistory(Long deviceId, int points);
}
