package com.nmap.dto;

import com.nmap.entity.ConfigFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DeviceConfigurationRequestDto {

    @NotBlank(message = "Configuration text cannot be blank")
    private String configText;

    @NotNull(message = "Configuration format is required")
    private ConfigFormat configFormat;

    @Size(max = 100, message = "Author cannot exceed 100 characters")
    private String author;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    public DeviceConfigurationRequestDto() {
    }

    public DeviceConfigurationRequestDto(String configText, ConfigFormat configFormat, String author, String description) {
        this.configText = configText;
        this.configFormat = configFormat;
        this.author = author;
        this.description = description;
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
}
