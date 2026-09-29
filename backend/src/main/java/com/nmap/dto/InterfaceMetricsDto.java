package com.nmap.dto;

public class InterfaceMetricsDto {

    private String interfaceName;
    private double inboundMbps;
    private double outboundMbps;
    private long capacityMbps;
    private double utilizationPercent;

    public InterfaceMetricsDto() {
    }

    public InterfaceMetricsDto(String interfaceName, double inboundMbps, double outboundMbps,
                               long capacityMbps, double utilizationPercent) {
        this.interfaceName = interfaceName;
        this.inboundMbps = inboundMbps;
        this.outboundMbps = outboundMbps;
        this.capacityMbps = capacityMbps;
        this.utilizationPercent = utilizationPercent;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public double getInboundMbps() {
        return inboundMbps;
    }

    public void setInboundMbps(double inboundMbps) {
        this.inboundMbps = inboundMbps;
    }

    public double getOutboundMbps() {
        return outboundMbps;
    }

    public void setOutboundMbps(double outboundMbps) {
        this.outboundMbps = outboundMbps;
    }

    public long getCapacityMbps() {
        return capacityMbps;
    }

    public void setCapacityMbps(long capacityMbps) {
        this.capacityMbps = capacityMbps;
    }

    public double getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(double utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }
}
