package com.cqs.qrmfg.exception;

public class WorkflowNotFoundException extends WorkflowException {
    public WorkflowNotFoundException(String message) {
        super(message);
    }
    
    public WorkflowNotFoundException(Long workflowId) {
        super(String.format("Workflow not found with ID: %d", workflowId));
    }
    
    public static WorkflowNotFoundException forMaterialId(String materialId) {
        return new WorkflowNotFoundException(String.format("Workflow not found for material: %s", materialId));
    }
}