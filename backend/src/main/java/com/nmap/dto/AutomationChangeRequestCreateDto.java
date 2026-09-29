package com.nmap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AutomationChangeRequestCreateDto {

    @NotNull(message = "Target device ID is required")
    private Long deviceId;

    @NotBlank(message = "Change request title cannot be blank")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Playbook ID cannot exceed 100 characters")
    private String playbookId;

    @NotBlank(message = "Configuration commands cannot be blank")
    private String configCommands;

    @Size(max = 100, message = "Author cannot exceed 100 characters")
    private String author;

    public AutomationChangeRequestCreateDto() {
    }

    public AutomationChangeRequestCreateDto(Long deviceId, String title, String description,
                                           String playbookId, String configCommands, String author) {
        this.deviceId = deviceId;
        this.title = title;
        this.description = description;
        this.playbookId = playbookId;
        this.configCommands = configCommands;
        this.author = author;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlaybookId() {
        return playbookId;
    }

    public void setPlaybookId(String playbookId) {
        this.playbookId = playbookId;
    }

    public String getConfigCommands() {
        return configCommands;
    }

    public void setConfigCommands(String configCommands) {
        this.configCommands = configCommands;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
