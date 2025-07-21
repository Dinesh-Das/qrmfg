package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.WorkflowState;
import java.util.List;
import java.util.Optional;

public interface WorkflowService {
    
    // Basic CRUD operations
    MaterialWorkflow save(MaterialWorkflow workflow);
    MaterialWorkflow update(MaterialWorkflow workflow);
    void delete(Long id);
    Optional<MaterialWorkflow> findById(Long id);
    Optional<MaterialWorkflow> findByMaterialId(String materialId);
    List<MaterialWorkflow> findAll();
    
    // Workflow creation
    MaterialWorkflow initiateWorkflow(String materialId, String materialName, String materialDescription, 
                                    String assignedPlant, String initiatedBy);
    MaterialWorkflow initiateWorkflow(String materialId, String assignedPlant, String initiatedBy);
    
    // State transition operations
    MaterialWorkflow transitionToState(Long workflowId, WorkflowState newState, String updatedBy);
    MaterialWorkflow transitionToState(String materialId, WorkflowState newState, String updatedBy);
    boolean canTransitionTo(Long workflowId, WorkflowState newState);
    boolean canTransitionTo(String materialId, WorkflowState newState);
    
    // Specific workflow actions
    MaterialWorkflow extendToPlant(Long workflowId, String updatedBy);
    MaterialWorkflow extendToPlant(String materialId, String updatedBy);
    MaterialWorkflow completeWorkflow(Long workflowId, String updatedBy);
    MaterialWorkflow completeWorkflow(String materialId, String updatedBy);
    MaterialWorkflow moveToQueryState(Long workflowId, WorkflowState queryState, String updatedBy);
    MaterialWorkflow returnFromQueryState(Long workflowId, String updatedBy);
    
    // Query-based operations
    List<MaterialWorkflow> findByState(WorkflowState state);
    List<MaterialWorkflow> findByAssignedPlant(String plantName);
    List<MaterialWorkflow> findByInitiatedBy(String username);
    List<MaterialWorkflow> findPendingWorkflows();
    List<MaterialWorkflow> findOverdueWorkflows();
    List<MaterialWorkflow> findWorkflowsWithOpenQueries();
    
    // Dashboard and reporting
    long countByState(WorkflowState state);
    long countOverdueWorkflows();
    long countWorkflowsWithOpenQueries();
    List<MaterialWorkflow> findRecentlyCreated(int days);
    List<MaterialWorkflow> findRecentlyCompleted(int days);
    
    // Validation and business rules
    void validateStateTransition(MaterialWorkflow workflow, WorkflowState newState);
    void validateWorkflowCompletion(MaterialWorkflow workflow);
    boolean isWorkflowReadyForCompletion(Long workflowId);
    boolean isWorkflowReadyForCompletion(String materialId);
}