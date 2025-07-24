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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/qrmfg/api/v1/workflows")
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
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getAllWorkflows() {
        List<MaterialWorkflow> workflows = workflowService.findAll();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowSummaryDto> getWorkflowById(@PathVariable Long id) {
        Optional<MaterialWorkflow> workflow = workflowService.findById(id);
        if (workflow.isPresent()) {
            MaterialWorkflow w = workflow.get();
            // Initialize collections within transaction
            w.getQueries().size();
            w.getDocuments().size();
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(w);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/material/{materialCode}")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowSummaryDto> getWorkflowByMaterialCode(@PathVariable String materialCode) {
        Optional<MaterialWorkflow> workflow = workflowService.findByMaterialCode(materialCode);
        if (workflow.isPresent()) {
            MaterialWorkflow w = workflow.get();
            // Initialize collections within transaction
            w.getQueries().size();
            w.getDocuments().size();
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(w);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    // Workflow creation - JVC users only
    @PostMapping
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> createWorkflow(
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
        
        // Initialize collections within transaction to avoid LazyInitializationException
        savedWorkflow.getQueries().size();
        savedWorkflow.getDocuments().size();
        
        // Convert to DTO to avoid serialization issues
        WorkflowSummaryDto workflowDto = workflowMapper.toSummaryDto(savedWorkflow);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowDto);
    }

    // State transition operations
    @PutMapping("/{id}/extend")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> extendToPlant(
            @PathVariable Long id,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.extendToPlant(id, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/material/{materialCode}/extend")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> extendToPlantByMaterialCode(
            @PathVariable String materialCode,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.extendToPlant(materialCode, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> completeWorkflow(
            @PathVariable Long id,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.completeWorkflow(id, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/material/{materialCode}/complete")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> completeWorkflowByMaterialCode(
            @PathVariable String materialCode,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        MaterialWorkflow workflow = workflowService.completeWorkflow(materialCode, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/transition")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> transitionToState(
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
            
            // Initialize collections within transaction
            workflow.getQueries().size();
            workflow.getDocuments().size();
            
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Query-based operations
    @GetMapping("/state/{state}")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByState(@PathVariable String state) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            List<MaterialWorkflow> workflows = workflowService.findByState(workflowState);
            
            // Debug logging
            System.out.println("Found " + workflows.size() + " workflows for state: " + state);
            for (MaterialWorkflow workflow : workflows) {
                System.out.println("Workflow: " + workflow.getId() + 
                    ", Project: " + workflow.getProjectCode() + 
                    ", Material: " + workflow.getMaterialCode() + 
                    ", Plant: " + workflow.getPlantCode() + 
                    ", Block: " + workflow.getBlockId() + 
                    ", Documents: " + (workflow.getDocuments() != null ? workflow.getDocuments().size() : 0));
            }
            
            List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
            
            // Debug the DTOs
            System.out.println("Mapped to " + workflowDtos.size() + " DTOs");
            for (WorkflowSummaryDto dto : workflowDtos) {
                System.out.println("DTO: " + dto.getId() + 
                    ", Project: " + dto.getProjectCode() + 
                    ", Material: " + dto.getMaterialCode() + 
                    ", Plant: " + dto.getPlantCode() + 
                    ", Block: " + dto.getBlockId() + 
                    ", Documents: " + dto.getDocumentCount());
            }
            
            return ResponseEntity.ok(workflowDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/plant/{plantName}")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByPlant(@PathVariable String plantName) {
        List<MaterialWorkflow> workflows = workflowService.findByPlantCode(plantName);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/initiated-by/{username}")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByInitiatedBy(@PathVariable String username) {
        List<MaterialWorkflow> workflows = workflowService.findByInitiatedBy(username);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getPendingWorkflows() {
        List<MaterialWorkflow> workflows = workflowService.findPendingWorkflows();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getOverdueWorkflows() {
        List<MaterialWorkflow> workflows = workflowService.findOverdueWorkflows();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/with-open-queries")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getRecentlyCreated(@RequestParam(defaultValue = "7") int days) {
        List<MaterialWorkflow> workflows = workflowService.findRecentlyCreated(days);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/recent/completed")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
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

    // Test endpoint to check workflow count
    @GetMapping("/test/count")
    public ResponseEntity<Map<String, Object>> getWorkflowCount() {
        try {
            List<MaterialWorkflow> allWorkflows = workflowService.findAll();
            List<MaterialWorkflow> jvcPendingWorkflows = workflowService.findByState(WorkflowState.JVC_PENDING);
            
            Map<String, Object> counts = new java.util.HashMap<>();
            counts.put("totalWorkflows", allWorkflows.size());
            counts.put("jvcPendingWorkflows", jvcPendingWorkflows.size());
            counts.put("sampleWorkflow", allWorkflows.isEmpty() ? null : allWorkflows.get(0));
            
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Test endpoint to create sample data
    @PostMapping("/test/create-sample-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createSampleData() {
        try {
            // Create a few sample workflows for testing
            MaterialWorkflow workflow1 = new MaterialWorkflow("SER-A-123456", "R12345A", "1001", "1001-A", "testuser");
            workflow1.setMaterialName("Test Material 1");
            workflow1.setMaterialDescription("Test material description 1");
            workflow1.setPriorityLevel("HIGH");
            
            MaterialWorkflow workflow2 = new MaterialWorkflow("SER-B-789012", "R67890B", "1002", "1002-B", "testuser");
            workflow2.setMaterialName("Test Material 2");
            workflow2.setMaterialDescription("Test material description 2");
            workflow2.setPriorityLevel("NORMAL");
            
            MaterialWorkflow workflow3 = new MaterialWorkflow("SER-C-345678", "R34567C", "1003", "1003-C", "admin");
            workflow3.setMaterialName("Test Material 3");
            workflow3.setMaterialDescription("Test material description 3");
            workflow3.setPriorityLevel("URGENT");
            
            workflowService.save(workflow1);
            workflowService.save(workflow2);
            workflowService.save(workflow3);
            
            return ResponseEntity.ok("Sample data created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating sample data: " + e.getMessage());
        }
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