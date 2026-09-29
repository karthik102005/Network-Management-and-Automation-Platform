package com.nmap.dto;

public class ConfigurationDiffLineDto {

    private DiffLineType type;
    private Integer oldLineNumber;
    private Integer newLineNumber;
    private String content;

    public ConfigurationDiffLineDto() {
    }

    public ConfigurationDiffLineDto(DiffLineType type, Integer oldLineNumber, Integer newLineNumber, String content) {
        this.type = type;
        this.oldLineNumber = oldLineNumber;
        this.newLineNumber = newLineNumber;
        this.content = content;
    }

    public DiffLineType getType() {
        return type;
    }

    public void setType(DiffLineType type) {
        this.type = type;
    }

    public Integer getOldLineNumber() {
        return oldLineNumber;
    }

    public void setOldLineNumber(Integer oldLineNumber) {
        this.oldLineNumber = oldLineNumber;
    }

    public Integer getNewLineNumber() {
        return newLineNumber;
    }

    public void setNewLineNumber(Integer newLineNumber) {
        this.newLineNumber = newLineNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
