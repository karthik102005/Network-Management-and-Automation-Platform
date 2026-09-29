package com.nmap.dto;

import com.nmap.entity.AdminStatus;
import com.nmap.entity.InterfaceType;
import com.nmap.entity.OperationalStatus;

import java.time.Instant;

public class InterfaceResponseDto {

    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private String interfaceName;
    private InterfaceType interfaceType;
    private String ipAddress;
    private Integer subnetPrefix;
    private String macAddress;
    private AdminStatus adminStatus;
    private OperationalStatus operationalStatus;
    private Long speedMbps;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public InterfaceResponseDto() {
    }

    public InterfaceResponseDto(Long id, Long deviceId, String deviceHostname, String interfaceName,
                                InterfaceType interfaceType, String ipAddress, Integer subnetPrefix,
                                String macAddress, AdminStatus adminStatus, OperationalStatus operationalStatus,
                                Long speedMbps, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceHostname = deviceHostname;
        this.interfaceName = interfaceName;
        this.interfaceType = interfaceType;
        this.ipAddress = ipAddress;
        this.subnetPrefix = subnetPrefix;
        this.macAddress = macAddress;
        this.adminStatus = adminStatus;
        this.operationalStatus = operationalStatus;
        this.speedMbps = speedMbps;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

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
}
