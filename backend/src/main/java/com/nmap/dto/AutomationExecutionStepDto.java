package com.nmap.dto;

import java.time.Instant;

public class AutomationExecutionStepDto {

    private int stepNumber;
    private String stepName;
    private String status;
    private String details;
    private Instant timestamp;

    public AutomationExecutionStepDto() {
    }

    public AutomationExecutionStepDto(int stepNumber, String stepName, String status, String details, Instant timestamp) {
        this.stepNumber = stepNumber;
        this.stepName = stepName;
        this.status = status;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
