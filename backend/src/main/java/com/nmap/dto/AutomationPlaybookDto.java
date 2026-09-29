package com.nmap.dto;

import java.util.List;

public class AutomationPlaybookDto {

    private String id;
    private String name;
    private String category;
    private String vendor;
    private String description;
    private List<String> variables;
    private String commandTemplate;

    public AutomationPlaybookDto() {
    }

    public AutomationPlaybookDto(String id, String name, String category, String vendor,
                                 String description, List<String> variables, String commandTemplate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.vendor = vendor;
        this.description = description;
        this.variables = variables;
        this.commandTemplate = commandTemplate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public String getCommandTemplate() {
        return commandTemplate;
    }

    public void setCommandTemplate(String commandTemplate) {
        this.commandTemplate = commandTemplate;
    }
}
