package com.nmap.dto;

import com.nmap.entity.ConfigFormat;
import java.util.List;

public class ConfigurationTemplateDto {

    private String id;
    private String name;
    private String vendor;
    private String deviceType;
    private ConfigFormat format;
    private String description;
    private String templateText;
    private String exampleWarning;
    private List<String> variables;

    public ConfigurationTemplateDto() {
    }

    public ConfigurationTemplateDto(String id, String name, String vendor, String deviceType,
                                    ConfigFormat format, String description, String templateText,
                                    String exampleWarning, List<String> variables) {
        this.id = id;
        this.name = name;
        this.vendor = vendor;
        this.deviceType = deviceType;
        this.format = format;
        this.description = description;
        this.templateText = templateText;
        this.exampleWarning = exampleWarning;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public ConfigFormat getFormat() {
        return format;
    }

    public void setFormat(ConfigFormat format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateText() {
        return templateText;
    }

    public void setTemplateText(String templateText) {
        this.templateText = templateText;
    }

    public String getExampleWarning() {
        return exampleWarning;
    }

    public void setExampleWarning(String exampleWarning) {
        this.exampleWarning = exampleWarning;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }
}
