package com.cqs.qrmfg.model;

public enum WorkflowState {
    JVC_PENDING("JVC Extension Required", "JVC team needs to extend material to plant"),
    PLANT_PENDING("Plant Questionnaire", "Plant team needs to complete questionnaire"),
    CQS_PENDING("CQS Query Resolution", "CQS team needs to resolve pending queries"),
    TECH_PENDING("Technology Query Resolution", "Technology team needs to resolve pending queries"),
    COMPLETED("Workflow Completed", "All steps completed successfully");

    private final String displayName;
    private final String description;

    WorkflowState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isQueryState() {
        return this == CQS_PENDING || this == TECH_PENDING;
    }

    public boolean isTerminalState() {
        return this == COMPLETED;
    }

    public boolean canTransitionTo(WorkflowState newState) {
        switch (this) {
            case JVC_PENDING:
                return newState == PLANT_PENDING;
            case PLANT_PENDING:
                return newState == CQS_PENDING || newState == TECH_PENDING || newState == COMPLETED;
            case CQS_PENDING:
            case TECH_PENDING:
                return newState == PLANT_PENDING;
            case COMPLETED:
                return false; // Terminal state
            default:
                return false;
        }
    }
}