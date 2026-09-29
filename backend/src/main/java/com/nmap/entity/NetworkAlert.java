package com.nmap.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "network_alerts",
    indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_device_id", columnList = "device_id"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_created_at", columnList = "created_at")
    }
)
public class NetworkAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private NetworkDevice device;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public NetworkAlert() {
    }

    public NetworkAlert(NetworkDevice device, AlertType alertType, AlertSeverity severity,
                        AlertStatus status, String message, String source) {
        this.device = device;
        this.alertType = alertType;
        this.severity = severity;
        this.status = status != null ? status : AlertStatus.OPEN;
        this.message = message;
        this.source = source;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = AlertStatus.OPEN;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NetworkDevice getDevice() {
        return device;
    }

    public void setDevice(NetworkDevice device) {
        this.device = device;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkAlert that = (NetworkAlert) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NetworkAlert{" +
                "id=" + id +
                ", alertType=" + alertType +
                ", severity=" + severity +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", source='" + source + '\'' +
                ", createdAt=" + createdAt +
                ", acknowledgedAt=" + acknowledgedAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}
