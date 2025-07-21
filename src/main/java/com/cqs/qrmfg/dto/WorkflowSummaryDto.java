package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.WorkflowState;

import java.time.LocalDateTime;

/**
 * DTO for workflow summary information
 */
public class WorkflowSummaryDto {
    private Long id;
    private String materialId;
    private String materialName;
    private WorkflowState currentState;
    private String assignedPlant;
    private String initiatedBy;
    private int daysPending;
    private long totalQueries;
    private long openQueries;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private boolean overdue;

    // Constructors
    public WorkflowSummaryDto() {}

    public WorkflowSummaryDto(Long id, String materialId, String materialName, WorkflowState currentState,
                             String assignedPlant, String initiatedBy, int daysPending, long totalQueries,
                             long openQueries, LocalDateTime createdAt, LocalDateTime lastModified, boolean overdue) {
        this.id = id;
        this.materialId = materialId;
        this.materialName = materialName;
        this.currentState = currentState;
        this.assignedPlant = assignedPlant;
        this.initiatedBy = initiatedBy;
        this.daysPending = daysPending;
        this.totalQueries = totalQueries;
        this.openQueries = openQueries;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.overdue = overdue;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public WorkflowState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(WorkflowState currentState) {
        this.currentState = currentState;
    }

    public String getAssignedPlant() {
        return assignedPlant;
    }

    public void setAssignedPlant(String assignedPlant) {
        this.assignedPlant = assignedPlant;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public int getDaysPending() {
        return daysPending;
    }

    public void setDaysPending(int daysPending) {
        this.daysPending = daysPending;
    }

    public long getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(long totalQueries) {
        this.totalQueries = totalQueries;
    }

    public long getOpenQueries() {
        return openQueries;
    }

    public void setOpenQueries(long openQueries) {
        this.openQueries = openQueries;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }
}