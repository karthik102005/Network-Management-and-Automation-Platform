package com.nmap.dto;

import com.nmap.entity.AlertSeverity;
import com.nmap.entity.AlertStatus;
import com.nmap.entity.AlertType;

import java.time.Instant;

public class AlertResponseDto {

    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private String deviceIp;
    private AlertType alertType;
    private AlertSeverity severity;
    private AlertStatus status;
    private String message;
    private String source;
    private Instant createdAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;

    public AlertResponseDto() {
    }

    public AlertResponseDto(Long id, Long deviceId, String deviceHostname, String deviceIp,
                            AlertType alertType, AlertSeverity severity, AlertStatus status,
                            String message, String source, Instant createdAt,
                            Instant acknowledgedAt, Instant resolvedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceHostname = deviceHostname;
        this.deviceIp = deviceIp;
        this.alertType = alertType;
        this.severity = severity;
        this.status = status;
        this.message = message;
        this.source = source;
        this.createdAt = createdAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
