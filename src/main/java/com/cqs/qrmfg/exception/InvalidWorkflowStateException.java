package com.cqs.qrmfg.exception;

import com.cqs.qrmfg.model.WorkflowState;

public class InvalidWorkflowStateException extends WorkflowException {
    public InvalidWorkflowStateException(String message) {
        super(message);
    }
    
    public InvalidWorkflowStateException(WorkflowState currentState, WorkflowState targetState) {
        super(String.format("Cannot transition from %s to %s", currentState, targetState));
    }
    
    public InvalidWorkflowStateException(String materialId, WorkflowState currentState, WorkflowState targetState) {
        super(String.format("Material %s: Cannot transition from %s to %s", materialId, currentState, targetState));
    }
}