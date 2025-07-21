package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.exception.InvalidWorkflowStateException;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.MaterialWorkflowRepository;
import com.cqs.qrmfg.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
    
    @Autowired
    private MaterialWorkflowRepository workflowRepository;
    
    // Basic CRUD operations
    @Override
    public MaterialWorkflow save(MaterialWorkflow workflow) {
        logger.debug("Saving workflow for material: {}", workflow.getMaterialId());
        return workflowRepository.save(workflow);
    }
    
    @Override
    public MaterialWorkflow update(MaterialWorkflow workflow) {
        if (workflow.getId() == null) {
            throw new WorkflowException("Cannot update workflow without ID");
        }
        
        Optional<MaterialWorkflow> existingOpt = workflowRepository.findById(workflow.getId());
        if (!existingOpt.isPresent()) {
            throw new WorkflowNotFoundException(workflow.getId());
        }
        
        logger.debug("Updating workflow for material: {}", workflow.getMaterialId());
        return workflowRepository.save(workflow);
    }
    
    @Override
    public void delete(Long id) {
        if (!workflowRepository.existsById(id)) {
            throw new WorkflowNotFoundException(id);
        }
        logger.debug("Deleting workflow with ID: {}", id);
        workflowRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<MaterialWorkflow> findById(Long id) {
        return workflowRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<MaterialWorkflow> findByMaterialId(String materialId) {
        return workflowRepository.findByMaterialId(materialId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findAll() {
        return workflowRepository.findAll();
    }
    
    // Workflow creation
    @Override
    public MaterialWorkflow initiateWorkflow(String materialId, String materialName, String materialDescription,
                                           String assignedPlant, String initiatedBy) {
        // Check if workflow already exists
        if (workflowRepository.existsByMaterialId(materialId)) {
            throw new WorkflowException("Workflow already exists for material: " + materialId);
        }
        
        MaterialWorkflow workflow = new MaterialWorkflow(materialId, initiatedBy, assignedPlant);
        workflow.setMaterialName(materialName);
        workflow.setMaterialDescription(materialDescription);
        
        logger.info("Initiating workflow for material: {} by user: {}", materialId, initiatedBy);
        return workflowRepository.save(workflow);
    }
    
    @Override
    public MaterialWorkflow initiateWorkflow(String materialId, String assignedPlant, String initiatedBy) {
        return initiateWorkflow(materialId, null, null, assignedPlant, initiatedBy);
    }
    
    // State transition operations
    @Override
    public MaterialWorkflow transitionToState(Long workflowId, WorkflowState newState, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return performStateTransition(workflow, newState, updatedBy);
    }
    
    @Override
    public MaterialWorkflow transitionToState(String materialId, WorkflowState newState, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialId(materialId)
            .orElseThrow(() -> WorkflowNotFoundException.forMaterialId(materialId));
        
        return performStateTransition(workflow, newState, updatedBy);
    }
    
    private MaterialWorkflow performStateTransition(MaterialWorkflow workflow, WorkflowState newState, String updatedBy) {
        WorkflowState currentState = workflow.getState();
        
        logger.info("Transitioning workflow {} from {} to {} by user: {}", 
                   workflow.getMaterialId(), currentState, newState, updatedBy);
        
        // Validate transition
        validateStateTransition(workflow, newState);
        
        // Perform transition
        workflow.transitionTo(newState, updatedBy);
        
        return workflowRepository.save(workflow);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canTransitionTo(Long workflowId, WorkflowState newState) {
        MaterialWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return workflow.canTransitionTo(newState);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canTransitionTo(String materialId, WorkflowState newState) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialId(materialId)
            .orElseThrow(() -> WorkflowNotFoundException.forMaterialId(materialId));
        
        return workflow.canTransitionTo(newState);
    }
    
    // Specific workflow actions
    @Override
    public MaterialWorkflow extendToPlant(Long workflowId, String updatedBy) {
        return transitionToState(workflowId, WorkflowState.PLANT_PENDING, updatedBy);
    }
    
    @Override
    public MaterialWorkflow extendToPlant(String materialId, String updatedBy) {
        return transitionToState(materialId, WorkflowState.PLANT_PENDING, updatedBy);
    }
    
    @Override
    public MaterialWorkflow completeWorkflow(Long workflowId, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        validateWorkflowCompletion(workflow);
        return transitionToState(workflowId, WorkflowState.COMPLETED, updatedBy);
    }
    
    @Override
    public MaterialWorkflow completeWorkflow(String materialId, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialId(materialId)
            .orElseThrow(() -> WorkflowNotFoundException.forMaterialId(materialId));
        
        validateWorkflowCompletion(workflow);
        return transitionToState(materialId, WorkflowState.COMPLETED, updatedBy);
    }
    
    @Override
    public MaterialWorkflow moveToQueryState(Long workflowId, WorkflowState queryState, String updatedBy) {
        if (!queryState.isQueryState()) {
            throw new InvalidWorkflowStateException("State " + queryState + " is not a query state");
        }
        return transitionToState(workflowId, queryState, updatedBy);
    }
    
    @Override
    public MaterialWorkflow returnFromQueryState(Long workflowId, String updatedBy) {
        return transitionToState(workflowId, WorkflowState.PLANT_PENDING, updatedBy);
    }
    
    // Query-based operations
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findByState(WorkflowState state) {
        return workflowRepository.findByState(state);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findByAssignedPlant(String plantName) {
        return workflowRepository.findByAssignedPlant(plantName);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findByInitiatedBy(String username) {
        return workflowRepository.findByInitiatedBy(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findPendingWorkflows() {
        return workflowRepository.findPendingWorkflows();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findOverdueWorkflows() {
        return workflowRepository.findOverdueWorkflows();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findWorkflowsWithOpenQueries() {
        return workflowRepository.findWorkflowsWithOpenQueries();
    }
    
    // Dashboard and reporting
    @Override
    @Transactional(readOnly = true)
    public long countByState(WorkflowState state) {
        return workflowRepository.countByState(state);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countOverdueWorkflows() {
        return workflowRepository.countOverdueWorkflows();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countWorkflowsWithOpenQueries() {
        return workflowRepository.countWorkflowsWithOpenQueries();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findRecentlyCreated(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return workflowRepository.findByCreatedAtAfter(cutoffDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findRecentlyCompleted(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return workflowRepository.findByCompletedAtAfter(cutoffDate);
    }
    
    // Validation and business rules
    @Override
    public void validateStateTransition(MaterialWorkflow workflow, WorkflowState newState) {
        if (!workflow.canTransitionTo(newState)) {
            throw new InvalidWorkflowStateException(workflow.getMaterialId(), workflow.getState(), newState);
        }
        
        // Additional business rule validations
        if (newState == WorkflowState.COMPLETED) {
            validateWorkflowCompletion(workflow);
        }
        
        // Validate query state transitions
        if (newState.isQueryState()) {
            if (workflow.getState() != WorkflowState.PLANT_PENDING) {
                throw new InvalidWorkflowStateException(
                    "Can only move to query states from PLANT_PENDING state");
            }
        }
    }
    
    @Override
    public void validateWorkflowCompletion(MaterialWorkflow workflow) {
        // Check if there are any open queries
        if (workflow.hasOpenQueries()) {
            throw new WorkflowException(
                String.format("Cannot complete workflow %s: %d open queries remaining", 
                             workflow.getMaterialId(), workflow.getOpenQueriesCount()));
        }
        
        // Check if workflow is in a valid state for completion
        if (workflow.getState() != WorkflowState.PLANT_PENDING) {
            throw new InvalidWorkflowStateException(
                "Workflow can only be completed from PLANT_PENDING state");
        }
        
        // Additional business rules can be added here
        // For example: check if all required questionnaire fields are filled
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isWorkflowReadyForCompletion(Long workflowId) {
        MaterialWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return isWorkflowReadyForCompletion(workflow);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isWorkflowReadyForCompletion(String materialId) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialId(materialId)
            .orElseThrow(() -> WorkflowNotFoundException.forMaterialId(materialId));
        
        return isWorkflowReadyForCompletion(workflow);
    }
    
    private boolean isWorkflowReadyForCompletion(MaterialWorkflow workflow) {
        try {
            validateWorkflowCompletion(workflow);
            return true;
        } catch (WorkflowException e) {
            return false;
        }
    }
}