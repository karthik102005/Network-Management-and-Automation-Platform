package com.nmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class DeviceMetricsDto {

    private Long deviceId;
    private String deviceHostname;
    private Instant timestamp;
    private double cpuUtilizationPercent;
    private double memoryUtilizationPercent;
    private double latencyMs;
    private double packetLossPercent;
    private DeviceHealthState healthState;
    private List<InterfaceMetricsDto> interfaceMetrics;
    private boolean simulated;
    private String telemetrySource;
    private String disclaimer;

    public DeviceMetricsDto() {
    }

    public DeviceMetricsDto(Long deviceId, String deviceHostname, Instant timestamp,
                            double cpuUtilizationPercent, double memoryUtilizationPercent,
                            double latencyMs, double packetLossPercent,
                            DeviceHealthState healthState, List<InterfaceMetricsDto> interfaceMetrics,
                            boolean simulated, String telemetrySource, String disclaimer) {
        this.deviceId = deviceId;
        this.deviceHostname = deviceHostname;
        this.timestamp = timestamp;
        this.cpuUtilizationPercent = cpuUtilizationPercent;
        this.memoryUtilizationPercent = memoryUtilizationPercent;
        this.latencyMs = latencyMs;
        this.packetLossPercent = packetLossPercent;
        this.healthState = healthState;
        this.interfaceMetrics = interfaceMetrics;
        this.simulated = simulated;
        this.telemetrySource = telemetrySource;
        this.disclaimer = disclaimer;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceHostname() {
        return deviceHostname;
    }

    public void setDeviceHostname(String deviceHostname) {
        this.deviceHostname = deviceHostname;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double getCpuUtilizationPercent() {
        return cpuUtilizationPercent;
    }

    public void setCpuUtilizationPercent(double cpuUtilizationPercent) {
        this.cpuUtilizationPercent = cpuUtilizationPercent;
    }

    public double getMemoryUtilizationPercent() {
        return memoryUtilizationPercent;
    }

    public void setMemoryUtilizationPercent(double memoryUtilizationPercent) {
        this.memoryUtilizationPercent = memoryUtilizationPercent;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public double getPacketLossPercent() {
        return packetLossPercent;
    }

    public void setPacketLossPercent(double packetLossPercent) {
        this.packetLossPercent = packetLossPercent;
    }

    public DeviceHealthState getHealthState() {
        return healthState;
    }

    public void setHealthState(DeviceHealthState healthState) {
        this.healthState = healthState;
    }

    public List<InterfaceMetricsDto> getInterfaceMetrics() {
        return interfaceMetrics;
    }

    public void setInterfaceMetrics(List<InterfaceMetricsDto> interfaceMetrics) {
        this.interfaceMetrics = interfaceMetrics;
    }

    @JsonProperty("simulated")
    public boolean isSimulated() {
        return simulated;
    }

    @JsonProperty("isSimulated")
    public boolean getIsSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getTelemetrySource() {
        return telemetrySource;
    }

    public void setTelemetrySource(String telemetrySource) {
        this.telemetrySource = telemetrySource;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
