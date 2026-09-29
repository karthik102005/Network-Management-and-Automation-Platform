package com.nmap.dto;

import java.time.Instant;

/**
 * Data Transfer Object representing the outcome of an on-demand device reachability probe.
 */
public class DeviceReachabilityResponseDto {

    private Long deviceId;
    private String hostname;
    private String managementIp;
    private boolean reachable;
    private String reachabilityStatus; // "REACHABLE", "UNREACHABLE", "INVALID_TARGET", "INCONCLUSIVE"
    private String probeMethod;         // "ICMP_ECHO", "TCP_CONNECTION", "TCP_RESET", "NONE"
    private Integer port;               // Port probed if TCP (e.g. 22, 80, 443, 23), null otherwise
    private Long responseTimeMs;        // Round-trip response time in ms, null if unreachable
    private Instant checkedAt;          // Timestamp when probe completed
    private String details;             // Diagnostic message explaining the result

    public DeviceReachabilityResponseDto() {
    }

    public DeviceReachabilityResponseDto(Long deviceId, String hostname, String managementIp,
                                        boolean reachable, String reachabilityStatus,
                                        String probeMethod, Integer port, Long responseTimeMs,
                                        Instant checkedAt, String details) {
        this.deviceId = deviceId;
        this.hostname = hostname;
        this.managementIp = managementIp;
        this.reachable = reachable;
        this.reachabilityStatus = reachabilityStatus;
        this.probeMethod = probeMethod;
        this.port = port;
        this.responseTimeMs = responseTimeMs;
        this.checkedAt = checkedAt;
        this.details = details;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public String getReachabilityStatus() {
        return reachabilityStatus;
    }

    public void setReachabilityStatus(String reachabilityStatus) {
        this.reachabilityStatus = reachabilityStatus;
    }

    public String getProbeMethod() {
        return probeMethod;
    }

    public void setProbeMethod(String probeMethod) {
        this.probeMethod = probeMethod;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getMessage() {
        return details;
    }
}
