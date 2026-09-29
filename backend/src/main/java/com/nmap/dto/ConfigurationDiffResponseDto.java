package com.nmap.dto;

import java.util.List;

public class ConfigurationDiffResponseDto {

    private Long deviceId;
    private String deviceHostname;
    private Integer v1;
    private Integer v2;
    private boolean identical;
    private int totalLinesV1;
    private int totalLinesV2;
    private int addedCount;
    private int removedCount;
    private int unchangedCount;
    private List<ConfigurationDiffLineDto> diffLines;

    public ConfigurationDiffResponseDto() {
    }

    public ConfigurationDiffResponseDto(Long deviceId, String deviceHostname, Integer v1, Integer v2,
                                        boolean identical, int totalLinesV1, int totalLinesV2,
                                        int addedCount, int removedCount, int unchangedCount,
                                        List<ConfigurationDiffLineDto> diffLines) {
        this.deviceId = deviceId;
        this.deviceHostname = deviceHostname;
        this.v1 = v1;
        this.v2 = v2;
        this.identical = identical;
        this.totalLinesV1 = totalLinesV1;
        this.totalLinesV2 = totalLinesV2;
        this.addedCount = addedCount;
        this.removedCount = removedCount;
        this.unchangedCount = unchangedCount;
        this.diffLines = diffLines;
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

    public Integer getV1() {
        return v1;
    }

    public void setV1(Integer v1) {
        this.v1 = v1;
    }

    public Integer getV2() {
        return v2;
    }

    public void setV2(Integer v2) {
        this.v2 = v2;
    }

    public boolean isIdentical() {
        return identical;
    }

    public void setIdentical(boolean identical) {
        this.identical = identical;
    }

    public int getTotalLinesV1() {
        return totalLinesV1;
    }

    public void setTotalLinesV1(int totalLinesV1) {
        this.totalLinesV1 = totalLinesV1;
    }

    public int getTotalLinesV2() {
        return totalLinesV2;
    }

    public void setTotalLinesV2(int totalLinesV2) {
        this.totalLinesV2 = totalLinesV2;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getRemovedCount() {
        return removedCount;
    }

    public void setRemovedCount(int removedCount) {
        this.removedCount = removedCount;
    }

    public int getUnchangedCount() {
        return unchangedCount;
    }

    public void setUnchangedCount(int unchangedCount) {
        this.unchangedCount = unchangedCount;
    }

    public List<ConfigurationDiffLineDto> getDiffLines() {
        return diffLines;
    }

    public void setDiffLines(List<ConfigurationDiffLineDto> diffLines) {
        this.diffLines = diffLines;
    }
}
