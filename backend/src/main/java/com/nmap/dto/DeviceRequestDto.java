package com.nmap.dto;

import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class DeviceRequestDto {

    @NotBlank(message = "Hostname is required")
    @Size(min = 1, max = 255, message = "Hostname must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Hostname may only contain alphanumeric characters, hyphens, dots, and underscores")
    private String hostname;

    @NotBlank(message = "Management IP is required")
    @Pattern(
        regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$",
        message = "Management IP must be a valid IPv4 address (e.g., 192.168.1.1)"
    )
    private String managementIp;

    @NotNull(message = "Device type is required (e.g. ROUTER, SWITCH, FIREWALL, ACCESS_POINT, SERVER, GATEWAY, LOAD_BALANCER, OTHER)")
    private DeviceType deviceType;

    @NotNull(message = "Vendor is required (e.g. CISCO, JUNIPER, ARISTA, MIKROTIK, HUAWEI, FORTINET, LINUX, GENERIC)")
    private DeviceVendor vendor;

    private DeviceStatus status;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Instant lastSeen;

    public DeviceRequestDto() {
    }

    public DeviceRequestDto(String hostname, String managementIp, DeviceType deviceType, DeviceVendor vendor, DeviceStatus status, String description) {
        this.hostname = hostname;
        this.managementIp = managementIp;
        this.deviceType = deviceType;
        this.vendor = vendor;
        this.status = status;
        this.description = description;
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

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}
