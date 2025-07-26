package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.WorkflowState;
import java.util.List;
import java.util.Optional;

public interface WorkflowService {
    
    // Basic CRUD operations
    Workflow save(Workflow workflow);
    Workflow update(Workflow workflow);
    void delete(Long id);
    Optional<Workflow> findById(Long id);
    Optional<Workflow> findByMaterialCode(String materialCode);
    List<Workflow> findAll();
    
    // Workflow creation
    Workflow initiateWorkflow(String materialCode, String materialName, String materialDescription, 
                            String assignedPlant, String initiatedBy);
    Workflow initiateWorkflow(String materialCode, String assignedPlant, String initiatedBy);
    
    // Enhanced workflow creation with project/material/plant/block structure
    Workflow initiateEnhancedWorkflow(String projectCode, String materialCode, String plantCode, 
                                    String blockId, String initiatedBy);
    
    // Material name management from ProjectItemMaster
    void updateMaterialNamesFromProjectItemMaster();
    Workflow updateMaterialNameFromProjectItemMaster(Long workflowId);
    
    // State transition operations
    Workflow transitionToState(Long workflowId, WorkflowState newState, String updatedBy);
    Workflow transitionToState(String materialCode, WorkflowState newState, String updatedBy);
    boolean canTransitionTo(Long workflowId, WorkflowState newState);
    boolean canTransitionTo(String materialCode, WorkflowState newState);
    
    // Specific workflow actions
    Workflow extendToPlant(Long workflowId, String updatedBy);
    Workflow extendToPlant(String materialCode, String updatedBy);
    Workflow completeWorkflow(Long workflowId, String updatedBy);
    Workflow completeWorkflow(String materialCode, String updatedBy);
    Workflow moveToQueryState(Long workflowId, WorkflowState queryState, String updatedBy);
    Workflow returnFromQueryState(Long workflowId, String updatedBy);
    
    // Query-based operations
    List<Workflow> findByState(WorkflowState state);
    List<Workflow> findByPlantCode(String plantCode);
    List<Workflow> findByInitiatedBy(String username);
    List<Workflow> findPendingWorkflows();
    List<Workflow> findOverdueWorkflows();
    List<Workflow> findWorkflowsWithOpenQueries();
    
    // Dashboard and reporting
    long countByState(WorkflowState state);
    long countOverdueWorkflows();
    long countWorkflowsWithOpenQueries();
    List<Workflow> findRecentlyCreated(int days);
    List<Workflow> findRecentlyCompleted(int days);
    
    // Validation and business rules
    void validateStateTransition(Workflow workflow, WorkflowState newState);
    void validateWorkflowCompletion(Workflow workflow);
    boolean isWorkflowReadyForCompletion(Long workflowId);
    boolean isWorkflowReadyForCompletion(String materialCode);
    
    // Duplicate checking
    Optional<Workflow> findExistingWorkflow(String projectCode, String materialCode, String plantCode, String blockId);
    boolean workflowExists(String projectCode, String materialCode, String plantCode, String blockId);
}