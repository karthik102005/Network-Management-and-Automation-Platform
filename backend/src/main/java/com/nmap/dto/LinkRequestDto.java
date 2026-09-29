package com.nmap.dto;

import com.nmap.entity.LinkStatus;
import com.nmap.entity.LinkType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LinkRequestDto {

    @NotNull(message = "Source interface ID is required")
    private Long sourceInterfaceId;

    @NotNull(message = "Destination interface ID is required")
    private Long destinationInterfaceId;

    private LinkStatus status = LinkStatus.UP;

    private LinkType linkType = LinkType.ETHERNET;

    @Min(value = 0, message = "Bandwidth must be non-negative")
    private Long bandwidthMbps;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    public LinkRequestDto() {
    }

    public LinkRequestDto(Long sourceInterfaceId, Long destinationInterfaceId, LinkStatus status,
                          LinkType linkType, Long bandwidthMbps, String description) {
        this.sourceInterfaceId = sourceInterfaceId;
        this.destinationInterfaceId = destinationInterfaceId;
        this.status = status != null ? status : LinkStatus.UP;
        this.linkType = linkType != null ? linkType : LinkType.ETHERNET;
        this.bandwidthMbps = bandwidthMbps;
        this.description = description;
    }

    // Getters and Setters

    public Long getSourceInterfaceId() {
        return sourceInterfaceId;
    }

    public void setSourceInterfaceId(Long sourceInterfaceId) {
        this.sourceInterfaceId = sourceInterfaceId;
    }

    public Long getDestinationInterfaceId() {
        return destinationInterfaceId;
    }

    public void setDestinationInterfaceId(Long destinationInterfaceId) {
        this.destinationInterfaceId = destinationInterfaceId;
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
}
