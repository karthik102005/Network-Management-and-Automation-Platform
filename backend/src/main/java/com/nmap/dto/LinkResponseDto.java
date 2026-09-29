package com.nmap.dto;

import com.nmap.entity.LinkStatus;
import com.nmap.entity.LinkType;

import java.time.Instant;

public class LinkResponseDto {

    private Long id;

    // Source Interface & Device
    private Long sourceInterfaceId;
    private String sourceInterfaceName;
    private Long sourceDeviceId;
    private String sourceHostname;

    // Destination Interface & Device
    private Long destinationInterfaceId;
    private String destinationInterfaceName;
    private Long destinationDeviceId;
    private String destinationHostname;

    private LinkStatus status;
    private LinkType linkType;
    private Long bandwidthMbps;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public LinkResponseDto() {
    }

    public LinkResponseDto(Long id, Long sourceInterfaceId, String sourceInterfaceName,
                           Long sourceDeviceId, String sourceHostname,
                           Long destinationInterfaceId, String destinationInterfaceName,
                           Long destinationDeviceId, String destinationHostname,
                           LinkStatus status, LinkType linkType, Long bandwidthMbps,
                           String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sourceInterfaceId = sourceInterfaceId;
        this.sourceInterfaceName = sourceInterfaceName;
        this.sourceDeviceId = sourceDeviceId;
        this.sourceHostname = sourceHostname;
        this.destinationInterfaceId = destinationInterfaceId;
        this.destinationInterfaceName = destinationInterfaceName;
        this.destinationDeviceId = destinationDeviceId;
        this.destinationHostname = destinationHostname;
        this.status = status;
        this.linkType = linkType;
        this.bandwidthMbps = bandwidthMbps;
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

    public Long getSourceInterfaceId() {
        return sourceInterfaceId;
    }

    public void setSourceInterfaceId(Long sourceInterfaceId) {
        this.sourceInterfaceId = sourceInterfaceId;
    }

    public String getSourceInterfaceName() {
        return sourceInterfaceName;
    }

    public void setSourceInterfaceName(String sourceInterfaceName) {
        this.sourceInterfaceName = sourceInterfaceName;
    }

    public Long getSourceDeviceId() {
        return sourceDeviceId;
    }

    public void setSourceDeviceId(Long sourceDeviceId) {
        this.sourceDeviceId = sourceDeviceId;
    }

    public String getSourceHostname() {
        return sourceHostname;
    }

    public void setSourceHostname(String sourceHostname) {
        this.sourceHostname = sourceHostname;
    }

    public Long getDestinationInterfaceId() {
        return destinationInterfaceId;
    }

    public void setDestinationInterfaceId(Long destinationInterfaceId) {
        this.destinationInterfaceId = destinationInterfaceId;
    }

    public String getDestinationInterfaceName() {
        return destinationInterfaceName;
    }

    public void setDestinationInterfaceName(String destinationInterfaceName) {
        this.destinationInterfaceName = destinationInterfaceName;
    }

    public Long getDestinationDeviceId() {
        return destinationDeviceId;
    }

    public void setDestinationDeviceId(Long destinationDeviceId) {
        this.destinationDeviceId = destinationDeviceId;
    }

    public String getDestinationHostname() {
        return destinationHostname;
    }

    public void setDestinationHostname(String destinationHostname) {
        this.destinationHostname = destinationHostname;
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
}
