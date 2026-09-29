package com.nmap.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "network_links",
    indexes = {
        @Index(name = "idx_link_src_if", columnList = "source_interface_id"),
        @Index(name = "idx_link_dst_if", columnList = "destination_interface_id"),
        @Index(name = "idx_link_status", columnList = "status")
    }
)
public class NetworkLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_interface_id", nullable = false, foreignKey = @ForeignKey(name = "fk_link_source_interface"))
    private NetworkInterface sourceInterface;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_interface_id", nullable = false, foreignKey = @ForeignKey(name = "fk_link_dest_interface"))
    private NetworkInterface destinationInterface;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LinkStatus status = LinkStatus.UP;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 30)
    private LinkType linkType = LinkType.ETHERNET;

    @Column(name = "bandwidth_mbps")
    private Long bandwidthMbps;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NetworkLink() {
    }

    public NetworkLink(NetworkInterface sourceInterface, NetworkInterface destinationInterface,
                       LinkStatus status, LinkType linkType, Long bandwidthMbps, String description) {
        this.sourceInterface = sourceInterface;
        this.destinationInterface = destinationInterface;
        this.status = status != null ? status : LinkStatus.UP;
        this.linkType = linkType != null ? linkType : LinkType.ETHERNET;
        this.bandwidthMbps = bandwidthMbps;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = LinkStatus.UP;
        if (this.linkType == null) this.linkType = LinkType.ETHERNET;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NetworkInterface getSourceInterface() {
        return sourceInterface;
    }

    public void setSourceInterface(NetworkInterface sourceInterface) {
        this.sourceInterface = sourceInterface;
    }

    public NetworkInterface getDestinationInterface() {
        return destinationInterface;
    }

    public void setDestinationInterface(NetworkInterface destinationInterface) {
        this.destinationInterface = destinationInterface;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public void setStatus(LinkStatus status) {
        this.status = status;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(LinkType linkType) {
        this.linkType = linkType;
    }

    public Long getBandwidthMbps() {
        return bandwidthMbps;
    }

    public void setBandwidthMbps(Long bandwidthMbps) {
        this.bandwidthMbps = bandwidthMbps;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkLink that = (NetworkLink) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NetworkLink{" +
                "id=" + id +
                ", status=" + status +
                ", linkType=" + linkType +
                ", bandwidthMbps=" + bandwidthMbps +
                '}';
    }
}
