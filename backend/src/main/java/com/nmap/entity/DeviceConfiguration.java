package com.nmap.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "device_configurations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_version", columnNames = {"device_id", "version"})
    },
    indexes = {
        @Index(name = "idx_config_device_id", columnList = "device_id"),
        @Index(name = "idx_config_active", columnList = "active"),
        @Index(name = "idx_config_created_at", columnList = "created_at")
    }
)
public class DeviceConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, foreignKey = @ForeignKey(name = "fk_config_device"))
    private NetworkDevice device;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "config_text", nullable = false, columnDefinition = "TEXT")
    private String configText;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_format", nullable = false, length = 30)
    private ConfigFormat configFormat = ConfigFormat.CISCO_IOS;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DeviceConfiguration() {
    }

    public DeviceConfiguration(NetworkDevice device, Integer version, String configText,
                               ConfigFormat configFormat, String checksum, Boolean active,
                               String author, String description) {
        this.device = device;
        this.version = version;
        this.configText = configText;
        this.configFormat = configFormat != null ? configFormat : ConfigFormat.CISCO_IOS;
        this.checksum = checksum;
        this.active = active != null ? active : true;
        this.author = author;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.active == null) {
            this.active = true;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceConfiguration that = (DeviceConfiguration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DeviceConfiguration{" +
                "id=" + id +
                ", version=" + version +
                ", configFormat=" + configFormat +
                ", checksum='" + checksum + '\'' +
                ", active=" + active +
                ", author='" + author + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
