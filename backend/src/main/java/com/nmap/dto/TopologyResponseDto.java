package com.nmap.dto;

import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.LinkStatus;
import com.nmap.entity.LinkType;

import java.util.List;

public class TopologyResponseDto {

    private List<TopologyNodeDto> nodes;
    private List<TopologyLinkDto> links;
    private int totalNodes;
    private int totalLinks;

    public TopologyResponseDto() {
    }

    public TopologyResponseDto(List<TopologyNodeDto> nodes, List<TopologyLinkDto> links) {
        this.nodes = nodes;
        this.links = links;
        this.totalNodes = nodes != null ? nodes.size() : 0;
        this.totalLinks = links != null ? links.size() : 0;
    }

    public List<TopologyNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<TopologyNodeDto> nodes) {
        this.nodes = nodes;
        this.totalNodes = nodes != null ? nodes.size() : 0;
    }

    public List<TopologyLinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<TopologyLinkDto> links) {
        this.links = links;
        this.totalLinks = links != null ? links.size() : 0;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public int getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(int totalLinks) {
        this.totalLinks = totalLinks;
    }

    public static class TopologyNodeDto {
        private Long id;
        private String hostname;
        private String managementIp;
        private DeviceType deviceType;
        private DeviceVendor vendor;
        private DeviceStatus status;
        private long interfaceCount;

        public TopologyNodeDto() {
        }

        public TopologyNodeDto(Long id, String hostname, String managementIp, DeviceType deviceType,
                               DeviceVendor vendor, DeviceStatus status, long interfaceCount) {
            this.id = id;
            this.hostname = hostname;
            this.managementIp = managementIp;
            this.deviceType = deviceType;
            this.vendor = vendor;
            this.status = status;
            this.interfaceCount = interfaceCount;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getManagementIp() {
            return managementIp;
        }

        public void setManagementIp(String managementIp) {
            this.managementIp = managementIp;
        }

        public DeviceType getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(DeviceType deviceType) {
            this.deviceType = deviceType;
        }

        public DeviceVendor getVendor() {
            return vendor;
        }

        public void setVendor(DeviceVendor vendor) {
            this.vendor = vendor;
        }

        public DeviceStatus getStatus() {
            return status;
        }

        public void setStatus(DeviceStatus status) {
            this.status = status;
        }

        public long getInterfaceCount() {
            return interfaceCount;
        }

        public void setInterfaceCount(long interfaceCount) {
            this.interfaceCount = interfaceCount;
        }
    }

    public static class TopologyLinkDto {
        private Long id;
        private Long sourceDeviceId;
        private String sourceHostname;
        private Long sourceInterfaceId;
        private String sourceInterfaceName;
        private Long destinationDeviceId;
        private String destinationHostname;
        private Long destinationInterfaceId;
        private String destinationInterfaceName;
        private LinkStatus status;
        private LinkType linkType;
        private Long bandwidthMbps;

        public TopologyLinkDto() {
        }

        public TopologyLinkDto(Long id, Long sourceDeviceId, String sourceHostname,
                               Long sourceInterfaceId, String sourceInterfaceName,
                               Long destinationDeviceId, String destinationHostname,
                               Long destinationInterfaceId, String destinationInterfaceName,
                               LinkStatus status, LinkType linkType, Long bandwidthMbps) {
            this.id = id;
            this.sourceDeviceId = sourceDeviceId;
            this.sourceHostname = sourceHostname;
            this.sourceInterfaceId = sourceInterfaceId;
            this.sourceInterfaceName = sourceInterfaceName;
            this.destinationDeviceId = destinationDeviceId;
            this.destinationHostname = destinationHostname;
            this.destinationInterfaceId = destinationInterfaceId;
            this.destinationInterfaceName = destinationInterfaceName;
            this.status = status;
            this.linkType = linkType;
            this.bandwidthMbps = bandwidthMbps;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
    }
}
