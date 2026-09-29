package com.nmap.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "automation_change_requests",
    indexes = {
        @Index(name = "idx_automation_device_id", columnList = "device_id"),
        @Index(name = "idx_automation_status", columnList = "status"),
        @Index(name = "idx_automation_created_at", columnList = "created_at")
    }
)
public class AutomationChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, foreignKey = @ForeignKey(name = "fk_automation_device"))
    private NetworkDevice device;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "playbook_id", length = 100)
    private String playbookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AutomationStatus status = AutomationStatus.DRAFT;

    @Column(name = "config_commands", nullable = false, columnDefinition = "TEXT")
    private String configCommands;

    @Column(name = "pre_change_version")
    private Integer preChangeVersion;

    @Column(name = "post_change_version")
    private Integer postChangeVersion;

    @Column(name = "rollback_version")
    private Integer rollbackVersion;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    @Column(name = "terminal_transcript", columnDefinition = "TEXT")
    private String terminalTranscript;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "is_simulated", nullable = false)
    private Boolean isSimulated = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public AutomationChangeRequest() {
    }

    public AutomationChangeRequest(NetworkDevice device, String title, String description,
                                   String playbookId, String configCommands, String author,
                                   Integer preChangeVersion) {
        this.device = device;
        this.title = title;
        this.description = description;
        this.playbookId = playbookId;
        this.configCommands = configCommands;
        this.author = author;
        this.preChangeVersion = preChangeVersion;
        this.status = AutomationStatus.DRAFT;
        this.isSimulated = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = AutomationStatus.DRAFT;
        }
        if (this.isSimulated == null) {
            this.isSimulated = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NetworkDevice getDevice() {
        return device;
    }

    public void setDevice(NetworkDevice device) {
        this.device = device;
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

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
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

    public Boolean getIsSimulated() {
        return isSimulated;
    }

    public void setIsSimulated(Boolean isSimulated) {
        this.isSimulated = isSimulated;
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
