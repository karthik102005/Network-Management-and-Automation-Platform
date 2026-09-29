package com.nmap.dto;

import com.nmap.entity.AdminStatus;
import com.nmap.entity.InterfaceType;
import com.nmap.entity.OperationalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class InterfaceRequestDto {

    @NotNull(message = "Device ID is required")
    private Long deviceId;

    @NotBlank(message = "Interface name is required")
    @Size(min = 1, max = 100, message = "Interface name must be between 1 and 100 characters")
    private String interfaceName;

    private InterfaceType interfaceType = InterfaceType.GIGABIT_ETHERNET;

    @Pattern(
        regexp = "^$|^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$",
        message = "IP address must be a valid IPv4 address (e.g., 192.168.1.1)"
    )
    private String ipAddress;

    @Min(value = 0, message = "Subnet prefix must be between 0 and 32")
    @Max(value = 32, message = "Subnet prefix must be between 0 and 32")
    private Integer subnetPrefix;

    @Pattern(
        regexp = "^$|^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$|^([0-9A-Fa-f]{4}\\.){2}[0-9A-Fa-f]{4}$",
        message = "MAC address must be in format XX:XX:XX:XX:XX:XX, XX-XX-XX-XX-XX-XX, or XXXX.XXXX.XXXX"
    )
    private String macAddress;

    private AdminStatus adminStatus = AdminStatus.UP;

    private OperationalStatus operationalStatus = OperationalStatus.UNKNOWN;

    @Min(value = 0, message = "Speed must be non-negative")
    private Long speedMbps;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    public InterfaceRequestDto() {
    }

    public InterfaceRequestDto(Long deviceId, String interfaceName, InterfaceType interfaceType,
                               String ipAddress, Integer subnetPrefix, String macAddress,
                               AdminStatus adminStatus, OperationalStatus operationalStatus,
                               Long speedMbps, String description) {
        this.deviceId = deviceId;
        this.interfaceName = interfaceName;
        this.interfaceType = interfaceType != null ? interfaceType : InterfaceType.GIGABIT_ETHERNET;
        this.ipAddress = ipAddress;
        this.subnetPrefix = subnetPrefix;
        this.macAddress = macAddress;
        this.adminStatus = adminStatus != null ? adminStatus : AdminStatus.UP;
        this.operationalStatus = operationalStatus != null ? operationalStatus : OperationalStatus.UNKNOWN;
        this.speedMbps = speedMbps;
        this.description = description;
    }

    // Getters and Setters

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public InterfaceType getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(InterfaceType interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getSubnetPrefix() {
        return subnetPrefix;
    }

    public void setSubnetPrefix(Integer subnetPrefix) {
        this.subnetPrefix = subnetPrefix;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public AdminStatus getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(AdminStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public Long getSpeedMbps() {
        return speedMbps;
    }

    public void setSpeedMbps(Long speedMbps) {
        this.speedMbps = speedMbps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
