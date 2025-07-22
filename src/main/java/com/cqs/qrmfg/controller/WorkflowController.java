package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.dto.WorkflowCreateRequest;
import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.service.DocumentService;
import com.cqs.qrmfg.service.WorkflowService;
import com.cqs.qrmfg.util.WorkflowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/qrmfg/api/v1/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private DocumentService documentService;

    // Basic CRUD operations
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getAllWorkflows() {
        List<MaterialWorkflow> workflows = workflowService.findAll();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MaterialWorkflow> getWorkflowById(@PathVariable Long id) {
        Optional<MaterialWorkflow> workflow = workflowService.findById(id);
        return workflow.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/material/{materialCode}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MaterialWorkflow> getWorkflowByMaterialCode(@PathVariable String materialCode) {
        Optional<MaterialWorkflow> workflow = workflowService.findByMaterialCode(materialCode);
        return workflow.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Workflow creation - JVC users only
    @PostMapping
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<MaterialWorkflow> createWorkflow(
            @Valid @RequestBody WorkflowCreateRequest request,
            Authentication authentication) {
        
        String initiatedBy = getCurrentUsername(authentication);
        
        MaterialWorkflow workflow;
        
        // Support both legacy and enhanced workflow creation
        if (request.getProjectCode() != null && request.getMaterialCode() != null && 
            request.getPlantCode() != null && request.getBlockId() != null) {
            // Enhanced workflow creation with project/material/plant/block structure
            workflow = workflowService.initiateEnhancedWorkflow(
                request.getProjectCode(),
                request.getMaterialCode(),
                request.getPlantCode(),
                request.getBlockId(),
                initiatedBy
            );
        } else {
            // Legacy workflow creation for backward compatibility
            workflow = workflowService.initiateWorkflow(
                request.getMaterialCode(),
                request.getMaterialName(),
                request.getMaterialDescription(),
                request.getAssignedPlant(),
                initiatedBy
            );
        }
        
        if (request.getSafetyDocumentsPath() != null) {
            workflow.setSafetyDocumentsPath(request.getSafetyDocumentsPath());
        }
        if (request.getPriorityLevel() != null) {
            workflow.setPriorityLevel(request.getPriorityLevel());
        }
        
        MaterialWorkflow savedWorkflow = workflowService.save(workflow);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWorkflow);
    }

    // State transition operations
    @PutMapping("/{id}/extend")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<MaterialWorkflow> extendToPlant(
            @PathVariable Long id,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.extendToPlant(id, updatedBy);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/material/{materialCode}/extend")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<MaterialWorkflow> extendToPlantByMaterialCode(
            @PathVariable String materialCode,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.extendToPlant(materialCode, updatedBy);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<MaterialWorkflow> completeWorkflow(
            @PathVariable Long id,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.completeWorkflow(id, updatedBy);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/material/{materialCode}/complete")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<MaterialWorkflow> completeWorkflowByMaterialCode(
            @PathVariable String materialCode,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.completeWorkflow(materialCode, updatedBy);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/{id}/transition")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaterialWorkflow> transitionToState(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String newStateStr = request.get("newState");
        if (newStateStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            WorkflowState newState = WorkflowState.valueOf(newStateStr);
            String updatedBy = getCurrentUsername(authentication);
            MaterialWorkflow workflow = workflowService.transitionToState(id, newState, updatedBy);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Query-based operations
    @GetMapping("/state/{state}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByState(@PathVariable String state) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            List<MaterialWorkflow> workflows = workflowService.findByState(workflowState);
            List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
            return ResponseEntity.ok(workflowDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/plant/{plantName}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByPlant(@PathVariable String plantName) {
        List<MaterialWorkflow> workflows = workflowService.findByPlantCode(plantName);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/initiated-by/{username}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByInitiatedBy(@PathVariable String username) {
        List<MaterialWorkflow> workflows = workflowService.findByInitiatedBy(username);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getPendingWorkflows() {
        List<MaterialWorkflow> workflows = workflowService.findPendingWorkflows();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getOverdueWorkflows() {
        List<MaterialWorkflow> workflows = workflowService.findOverdueWorkflows();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/with-open-queries")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsWithOpenQueries() {
        List<MaterialWorkflow> workflows = workflowService.findWorkflowsWithOpenQueries();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    // Dashboard and reporting endpoints
    @GetMapping("/stats/count-by-state/{state}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getCountByState(@PathVariable String state) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            long count = workflowService.countByState(workflowState);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/overdue-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getOverdueCount() {
        long count = workflowService.countOverdueWorkflows();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/with-open-queries-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getWorkflowsWithOpenQueriesCount() {
        long count = workflowService.countWorkflowsWithOpenQueries();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/recent/created")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getRecentlyCreated(@RequestParam(defaultValue = "7") int days) {
        List<MaterialWorkflow> workflows = workflowService.findRecentlyCreated(days);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/recent/completed")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkflowSummaryDto>> getRecentlyCompleted(@RequestParam(defaultValue = "7") int days) {
        List<MaterialWorkflow> workflows = workflowService.findRecentlyCompleted(days);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    // Validation endpoints
    @GetMapping("/{id}/can-transition/{newState}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> canTransitionTo(@PathVariable Long id, @PathVariable String newState) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(newState);
            boolean canTransition = workflowService.canTransitionTo(id, workflowState);
            return ResponseEntity.ok(canTransition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/ready-for-completion")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> isReadyForCompletion(@PathVariable Long id) {
        boolean isReady = workflowService.isWorkflowReadyForCompletion(id);
        return ResponseEntity.ok(isReady);
    }

    // Document access endpoints
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> getWorkflowDocuments(@PathVariable Long id) {
        List<DocumentSummary> documents = documentService.getWorkflowDocuments(id);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/reusable")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> getReusableDocuments(
            @RequestParam String projectCode, 
            @RequestParam String materialCode) {
        List<DocumentSummary> documents = documentService.getReusableDocuments(projectCode, materialCode);
        return ResponseEntity.ok(documents);
    }

    // Utility method to get current username
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return "SYSTEM";
    }

    // Exception handlers
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWorkflowNotFound(WorkflowNotFoundException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Workflow not found");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<Map<String, String>> handleWorkflowException(WorkflowException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Workflow error");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid argument");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}