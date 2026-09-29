package com.nmap.dto;

import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;

import java.time.Instant;

public class DeviceResponseDto {

    private Long id;
    private String hostname;
    private String managementIp;
    private DeviceType deviceType;
    private DeviceVendor vendor;
    private DeviceStatus status;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSeen;

    public DeviceResponseDto() {
    }

    public DeviceResponseDto(Long id, String hostname, String managementIp, DeviceType deviceType,
                             DeviceVendor vendor, DeviceStatus status, String description,
                             Instant createdAt, Instant updatedAt, Instant lastSeen) {
        this.id = id;
        this.hostname = hostname;
        this.managementIp = managementIp;
        this.deviceType = deviceType;
        this.vendor = vendor;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastSeen = lastSeen;
    }

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
}
