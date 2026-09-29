package com.nmap.dto;

import com.nmap.entity.ConfigFormat;
import java.time.Instant;

public class DeviceConfigurationResponseDto {

    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private Integer version;
    private String configText;
    private ConfigFormat configFormat;
    private String checksum;
    private Boolean active;
    private String author;
    private String description;
    private Instant createdAt;

    public DeviceConfigurationResponseDto() {
    }

    public DeviceConfigurationResponseDto(Long id, Long deviceId, String deviceHostname, Integer version,
                                          String configText, ConfigFormat configFormat, String checksum,
                                          Boolean active, String author, String description, Instant createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceHostname = deviceHostname;
        this.version = version;
        this.configText = configText;
        this.configFormat = configFormat;
        this.checksum = checksum;
        this.active = active;
        this.author = author;
        this.description = description;
        this.createdAt = createdAt;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getConfigText() {
        return configText;
    }

    public void setConfigText(String configText) {
        this.configText = configText;
    }

    public ConfigFormat getConfigFormat() {
        return configFormat;
    }

    public void setConfigFormat(ConfigFormat configFormat) {
        this.configFormat = configFormat;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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
}
