package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.exception.InvalidWorkflowStateException;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.service.WorkflowService;
import com.cqs.qrmfg.service.MetricsService;
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
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private MetricsService metricsService;
    
    // Basic CRUD operations
    @Override
    public MaterialWorkflow save(MaterialWorkflow workflow) {
        logger.debug("Saving workflow for material: {}", workflow.getMaterialCode());
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
        
        logger.debug("Updating workflow for material: {}", workflow.getMaterialCode());
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
    public Optional<MaterialWorkflow> findByMaterialCode(String materialCode) {
        // If you want to keep returning Optional, use stream().findFirst()
        return workflowRepository.findByMaterialCode(materialCode).stream().findFirst();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findAll() {
        List<MaterialWorkflow> workflows = workflowRepository.findAllWithQueries();
        
        // Initialize documents collection for each workflow within the same transaction
        for (MaterialWorkflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    // Workflow creation
    @Override
    public MaterialWorkflow initiateWorkflow(String materialCode, String materialName, String materialDescription,
                                           String assignedPlant, String initiatedBy) {
        // Check if workflow already exists
        if (workflowRepository.existsByMaterialCode(materialCode)) {
            throw new WorkflowException("Workflow already exists for material: " + materialCode);
        }
        
        // Create workflow using enhanced constructor and map to legacy fields
        MaterialWorkflow workflow = new MaterialWorkflow(materialCode, materialCode, assignedPlant, "DEFAULT", initiatedBy);
        workflow.setMaterialName(materialName);
        workflow.setMaterialDescription(materialDescription);
        
        logger.info("Initiating workflow for material: {} by user: {}", materialCode, initiatedBy);
        MaterialWorkflow savedWorkflow = workflowRepository.save(workflow);
        
        // Send notification for workflow creation
        try {
            notificationService.notifyWorkflowCreated(savedWorkflow);
        } catch (Exception e) {
            logger.warn("Failed to send workflow creation notification for material {}: {}", 
                       materialCode, e.getMessage());
        }
        
        return savedWorkflow;
    }
    
    @Override
    public MaterialWorkflow initiateWorkflow(String materialCode, String assignedPlant, String initiatedBy) {
        return initiateWorkflow(materialCode, null, null, assignedPlant, initiatedBy);
    }

    @Override
    public MaterialWorkflow initiateEnhancedWorkflow(String projectCode, String materialCode, String plantCode, 
                                                   String blockId, String initiatedBy) {
        // Check if workflow already exists for this combination
        if (workflowRepository.existsByProjectCodeAndMaterialCodeAndPlantCodeAndBlockId(
                projectCode, materialCode, plantCode, blockId)) {
            throw new WorkflowException(String.format(
                "Workflow already exists for project: %s, material: %s, plant: %s, block: %s", 
                projectCode, materialCode, plantCode, blockId));
        }
        
        MaterialWorkflow workflow = new MaterialWorkflow(projectCode, materialCode, plantCode, blockId, initiatedBy);
        
        logger.info("Initiating enhanced workflow for project: {}, material: {}, plant: {}, block: {} by user: {}", 
                   projectCode, materialCode, plantCode, blockId, initiatedBy);
        MaterialWorkflow savedWorkflow = workflowRepository.save(workflow);
        
        // Send notification for workflow creation
        try {
            notificationService.notifyWorkflowCreated(savedWorkflow);
        } catch (Exception e) {
            logger.warn("Failed to send workflow creation notification for project {}, material {}: {}", 
                       projectCode, materialCode, e.getMessage());
        }
        
        return savedWorkflow;
    }
    
    // State transition operations
    @Override
    public MaterialWorkflow transitionToState(Long workflowId, WorkflowState newState, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return performStateTransition(workflow, newState, updatedBy);
    }
    
    @Override
    public MaterialWorkflow transitionToState(String materialCode, WorkflowState newState, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return performStateTransition(workflow, newState, updatedBy);
    }
    
    private MaterialWorkflow performStateTransition(MaterialWorkflow workflow, WorkflowState newState, String updatedBy) {
        WorkflowState currentState = workflow.getState();
        
        logger.info("Transitioning workflow {} from {} to {} by user: {}", 
                   workflow.getMaterialCode(), currentState, newState, updatedBy);
        
        // Validate transition
        validateStateTransition(workflow, newState);
        
        // Perform transition
        workflow.transitionTo(newState, updatedBy);
        
        MaterialWorkflow savedWorkflow = workflowRepository.save(workflow);
        
        // Send notification for state change - this is the core integration point
        try {
            notificationService.notifyWorkflowStateChanged(savedWorkflow, currentState, updatedBy);
            
            // Send specific notifications based on the new state
            switch (newState) {
                case PLANT_PENDING:
                    // Additional notification when workflow moves to plant
                    if (currentState == WorkflowState.JVC_PENDING) {
                        notificationService.notifyWorkflowExtended(savedWorkflow, updatedBy);
                    }
                    break;
                case COMPLETED:
                    // Additional notification when workflow completes
                    notificationService.notifyWorkflowCompleted(savedWorkflow, updatedBy);
                    break;
                default:
                    // State change notification already sent above
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to send workflow state change notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return savedWorkflow;
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
    public boolean canTransitionTo(String materialCode, WorkflowState newState) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return workflow.canTransitionTo(newState);
    }
    
    // Specific workflow actions
    @Override
    public MaterialWorkflow extendToPlant(Long workflowId, String updatedBy) {
        MaterialWorkflow workflow = transitionToState(workflowId, WorkflowState.PLANT_PENDING, updatedBy);
        
        // Send specific notification for workflow extension
        try {
            notificationService.notifyWorkflowExtended(workflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow extension notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return workflow;
    }
    
    @Override
    public MaterialWorkflow extendToPlant(String materialCode, String updatedBy) {
        MaterialWorkflow workflow = transitionToState(materialCode, WorkflowState.PLANT_PENDING, updatedBy);
        
        // Send specific notification for workflow extension
        try {
            notificationService.notifyWorkflowExtended(workflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow extension notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return workflow;
    }
    
    @Override
    public MaterialWorkflow completeWorkflow(Long workflowId, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        validateWorkflowCompletion(workflow);
        MaterialWorkflow completedWorkflow = transitionToState(workflowId, WorkflowState.COMPLETED, updatedBy);
        
        // Send specific notification for workflow completion
        try {
            notificationService.notifyWorkflowCompleted(completedWorkflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow completion notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return completedWorkflow;
    }
    
    @Override
    public MaterialWorkflow completeWorkflow(String materialCode, String updatedBy) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        validateWorkflowCompletion(workflow);
        MaterialWorkflow completedWorkflow = transitionToState(materialCode, WorkflowState.COMPLETED, updatedBy);
        
        // Send specific notification for workflow completion
        try {
            notificationService.notifyWorkflowCompleted(completedWorkflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow completion notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return completedWorkflow;
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
        List<MaterialWorkflow> workflows = workflowRepository.findByStateWithQueries(state);
        
        // Initialize documents collection for each workflow within the same transaction
        for (MaterialWorkflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findByPlantCode(String plantCode) {
        List<MaterialWorkflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
        
        // Initialize documents collection for each workflow within the same transaction
        for (MaterialWorkflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MaterialWorkflow> findByInitiatedBy(String username) {
        List<MaterialWorkflow> workflows = workflowRepository.findByInitiatedByWithQueries(username);
        
        // Initialize documents collection for each workflow within the same transaction
        for (MaterialWorkflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
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
            throw new InvalidWorkflowStateException(
                String.format("Invalid state transition for workflow %s: %s -> %s", workflow.getMaterialCode(), workflow.getState(), newState)
            );
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
                             workflow.getMaterialCode(), workflow.getOpenQueriesCount()));
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
    public boolean isWorkflowReadyForCompletion(String materialCode) {
        MaterialWorkflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
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
    
    // Duplicate checking methods
    @Override
    @Transactional(readOnly = true)
    public Optional<MaterialWorkflow> findExistingWorkflow(String projectCode, String materialCode, String plantCode, String blockId) {
        Optional<MaterialWorkflow> workflow = workflowRepository.findByProjectCodeAndMaterialCodeAndPlantCodeAndBlockIdWithQueries(
            projectCode, materialCode, plantCode, blockId);
        
        // Initialize documents collection within transaction if workflow exists
        if (workflow.isPresent()) {
            workflow.get().getDocuments().size(); // Trigger lazy loading
        }
        
        return workflow;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean workflowExists(String projectCode, String materialCode, String plantCode, String blockId) {
        return workflowRepository.existsByProjectCodeAndMaterialCodeAndPlantCodeAndBlockId(
            projectCode, materialCode, plantCode, blockId);
    }
}