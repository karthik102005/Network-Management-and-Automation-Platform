package com.nmap.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "network_devices",
    indexes = {
        @Index(name = "idx_device_hostname", columnList = "hostname", unique = true),
        @Index(name = "idx_device_mgmt_ip", columnList = "management_ip", unique = true),
        @Index(name = "idx_device_status", columnList = "status"),
        @Index(name = "idx_device_type", columnList = "device_type")
    }
)
public class NetworkDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hostname", nullable = false, unique = true, length = 255)
    private String hostname;

    @Column(name = "management_ip", nullable = false, unique = true, length = 45)
    private String managementIp;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 50)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor", nullable = false, length = 50)
    private DeviceVendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeviceStatus status = DeviceStatus.UNKNOWN;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_seen")
    private Instant lastSeen;

    public NetworkDevice() {
    }

    public NetworkDevice(String hostname, String managementIp, DeviceType deviceType, DeviceVendor vendor, DeviceStatus status, String description) {
        this.hostname = hostname;
        this.managementIp = managementIp;
        this.deviceType = deviceType;
        this.vendor = vendor;
        this.status = status != null ? status : DeviceStatus.UNKNOWN;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = DeviceStatus.UNKNOWN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public DeviceVendor getVendor() {
        return vendor;
    }

    public void setVendor(DeviceVendor vendor) {
        this.vendor = vendor;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkDevice that = (NetworkDevice) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(hostname, that.hostname) &&
               Objects.equals(managementIp, that.managementIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hostname, managementIp);
    }

    @Override
    public String toString() {
        return "NetworkDevice{" +
                "id=" + id +
                ", hostname='" + hostname + '\'' +
                ", managementIp='" + managementIp + '\'' +
                ", deviceType=" + deviceType +
                ", vendor=" + vendor +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastSeen=" + lastSeen +
                '}';
    }
}
