package com.nmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nmap.entity.AutomationStatus;

import java.time.Instant;
import java.util.List;

public class AutomationChangeRequestResponseDto {

    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private String title;
    private String description;
    private String playbookId;
    private AutomationStatus status;
    private String configCommands;
    private Integer preChangeVersion;
    private Integer postChangeVersion;
    private Integer rollbackVersion;
    private String author;
    private List<AutomationExecutionStepDto> executionSteps;
    private String terminalTranscript;
    private String errorMessage;
    private boolean simulated = true;
    private String disclaimer = "Simulated automation execution. No live hardware commands issued.";
    private Instant createdAt;
    private Instant executedAt;
    private Instant completedAt;

    public AutomationChangeRequestResponseDto() {
    }

    public AutomationChangeRequestResponseDto(Long id, Long deviceId, String deviceHostname, String title,
                                             String description, String playbookId, AutomationStatus status,
                                             String configCommands, Integer preChangeVersion,
                                             Integer postChangeVersion, Integer rollbackVersion,
                                             String author, List<AutomationExecutionStepDto> executionSteps,
                                             String terminalTranscript, String errorMessage,
                                             boolean simulated, String disclaimer,
                                             Instant createdAt, Instant executedAt, Instant completedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceHostname = deviceHostname;
        this.title = title;
        this.description = description;
        this.playbookId = playbookId;
        this.status = status;
        this.configCommands = configCommands;
        this.preChangeVersion = preChangeVersion;
        this.postChangeVersion = postChangeVersion;
        this.rollbackVersion = rollbackVersion;
        this.author = author;
        this.executionSteps = executionSteps;
        this.terminalTranscript = terminalTranscript;
        this.errorMessage = errorMessage;
        this.simulated = simulated;
        this.disclaimer = disclaimer;
        this.createdAt = createdAt;
        this.executedAt = executedAt;
        this.completedAt = completedAt;
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

    public AutomationStatus getStatus() {
        return status;
    }

    public void setStatus(AutomationStatus status) {
        this.status = status;
    }

    public String getConfigCommands() {
        return configCommands;
    }

    public void setConfigCommands(String configCommands) {
        this.configCommands = configCommands;
    }

    public Integer getPreChangeVersion() {
        return preChangeVersion;
    }

    public void setPreChangeVersion(Integer preChangeVersion) {
        this.preChangeVersion = preChangeVersion;
    }

    public Integer getPostChangeVersion() {
        return postChangeVersion;
    }

    public void setPostChangeVersion(Integer postChangeVersion) {
        this.postChangeVersion = postChangeVersion;
    }

    public Integer getRollbackVersion() {
        return rollbackVersion;
    }

    public void setRollbackVersion(Integer rollbackVersion) {
        this.rollbackVersion = rollbackVersion;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<AutomationExecutionStepDto> getExecutionSteps() {
        return executionSteps;
    }

    public void setExecutionSteps(List<AutomationExecutionStepDto> executionSteps) {
        this.executionSteps = executionSteps;
    }

    public String getTerminalTranscript() {
        return terminalTranscript;
    }

    public void setTerminalTranscript(String terminalTranscript) {
        this.terminalTranscript = terminalTranscript;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @JsonProperty("simulated")
    public boolean isSimulated() {
        return simulated;
    }

    @JsonProperty("isSimulated")
    public boolean getIsSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
