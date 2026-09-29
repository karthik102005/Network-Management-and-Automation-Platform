package com.nmap.dto;

import java.time.Instant;

public class DeviceMetricsHistoryPointDto {

    private Instant timestamp;
    private double cpuPercent;
    private double memoryPercent;
    private double latencyMs;
    private double inboundMbps;
    private double outboundMbps;

    public DeviceMetricsHistoryPointDto() {
    }

    public DeviceMetricsHistoryPointDto(Instant timestamp, double cpuPercent, double memoryPercent,
                                       double latencyMs, double inboundMbps, double outboundMbps) {
        this.timestamp = timestamp;
        this.cpuPercent = cpuPercent;
        this.memoryPercent = memoryPercent;
        this.latencyMs = latencyMs;
        this.inboundMbps = inboundMbps;
        this.outboundMbps = outboundMbps;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(double cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public double getMemoryPercent() {
        return memoryPercent;
    }

    public void setMemoryPercent(double memoryPercent) {
        this.memoryPercent = memoryPercent;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public double getInboundMbps() {
        return inboundMbps;
    }

    public void setInboundMbps(double inboundMbps) {
        this.inboundMbps = inboundMbps;
    }

    public double getOutboundMbps() {
        return outboundMbps;
    }

    public void setOutboundMbps(double outboundMbps) {
        this.outboundMbps = outboundMbps;
    }
}
