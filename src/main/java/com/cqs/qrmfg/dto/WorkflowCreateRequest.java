package com.cqs.qrmfg.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class WorkflowCreateRequest {
    @NotBlank(message = "Material ID is required")
    @Size(max = 100, message = "Material ID must not exceed 100 characters")
    private String materialId;

    @Size(max = 200, message = "Material name must not exceed 200 characters")
    private String materialName;

    @Size(max = 1000, message = "Material description must not exceed 1000 characters")
    private String materialDescription;

    @NotBlank(message = "Assigned plant is required")
    @Size(max = 100, message = "Assigned plant must not exceed 100 characters")
    private String assignedPlant;

    @Size(max = 500, message = "Safety documents path must not exceed 500 characters")
    private String safetyDocumentsPath;

    @Size(max = 20, message = "Priority level must not exceed 20 characters")
    private String priorityLevel = "NORMAL";

    public WorkflowCreateRequest() {}

    public WorkflowCreateRequest(String materialId, String materialName, String materialDescription,
                               String assignedPlant) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.materialDescription = materialDescription;
        this.assignedPlant = assignedPlant;
    }

    // Getters and setters
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialDescription() { return materialDescription; }
    public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

    public String getAssignedPlant() { return assignedPlant; }
    public void setAssignedPlant(String assignedPlant) { this.assignedPlant = assignedPlant; }

    public String getSafetyDocumentsPath() { return safetyDocumentsPath; }
    public void setSafetyDocumentsPath(String safetyDocumentsPath) { this.safetyDocumentsPath = safetyDocumentsPath; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
}