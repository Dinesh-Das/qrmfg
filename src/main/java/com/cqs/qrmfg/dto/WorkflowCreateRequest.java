package com.cqs.qrmfg.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class WorkflowCreateRequest {
    // Legacy fields for backward compatibility
    @Size(max = 100, message = "Material code must not exceed 100 characters")
    private String materialCode;

    @Size(max = 200, message = "Material name must not exceed 200 characters")
    private String materialName;

    @Size(max = 1000, message = "Material description must not exceed 1000 characters")
    private String materialDescription;

    @Size(max = 100, message = "Assigned plant must not exceed 100 characters")
    private String assignedPlant;

    // Enhanced workflow fields
    @NotBlank(message = "Project code is required")
    @Size(max = 50, message = "Project code must not exceed 50 characters")
    private String projectCode;

    @NotBlank(message = "Plant code is required")
    @Size(max = 50, message = "Plant code must not exceed 50 characters")
    private String plantCode;

    @NotBlank(message = "Block ID is required")
    @Size(max = 50, message = "Block ID must not exceed 50 characters")
    private String blockId;

    @Size(max = 500, message = "Safety documents path must not exceed 500 characters")
    private String safetyDocumentsPath;

    @Size(max = 20, message = "Priority level must not exceed 20 characters")
    private String priorityLevel = "NORMAL";

    public WorkflowCreateRequest() {}

    public WorkflowCreateRequest(String materialCode, String materialName, String materialDescription,
                               String assignedPlant) {
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.materialDescription = materialDescription;
        this.assignedPlant = assignedPlant;
    }

    public WorkflowCreateRequest(String projectCode, String materialCode, String plantCode, String blockId, String dummy) {
        this.projectCode = projectCode;
        this.materialCode = materialCode;
        this.plantCode = plantCode;
        this.blockId = blockId;
    }

    // Getters and setters
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialDescription() { return materialDescription; }
    public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

    public String getAssignedPlant() { return assignedPlant; }
    public void setAssignedPlant(String assignedPlant) { this.assignedPlant = assignedPlant; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }

    public String getSafetyDocumentsPath() { return safetyDocumentsPath; }
    public void setSafetyDocumentsPath(String safetyDocumentsPath) { this.safetyDocumentsPath = safetyDocumentsPath; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
}