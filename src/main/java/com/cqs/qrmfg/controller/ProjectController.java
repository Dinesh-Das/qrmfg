package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for project and reference data APIs
 */
@RestController
@RequestMapping("/qrmfg/api/v1/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * Get all active projects
     * Query: SELECT DISTINCT object_key as PROJECT_CODE, object_key as label FROM fsobjectreference 
     * WHERE object_type='PROJECT' AND object_key LIKE 'SER%'
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProjectOption>> getActiveProjects() {
        List<ProjectOption> projects = projectService.getActiveProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get materials for a specific project
     * Query: SELECT r_object_key as MATERIAL_CODE, r_object_desc as label FROM fsobjectreference 
     * WHERE object_type='PROJECT' AND object_key = :projectCode AND r_object_type='ITEM' AND ref_code='SER_PRD_ITEM'
     */
    @GetMapping("/{projectCode}/materials")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MaterialOption>> getMaterialsByProject(@PathVariable String projectCode) {
        List<MaterialOption> materials = projectService.getMaterialsByProject(projectCode);
        return ResponseEntity.ok(materials);
    }

    /**
     * Get all plant codes
     * Query: SELECT location_code as value, location_code as label FROM fslocation WHERE location_code LIKE '1%'
     */
    @GetMapping("/plants")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PlantOption>> getPlantCodes() {
        List<PlantOption> plants = projectService.getPlantCodes();
        return ResponseEntity.ok(plants);
    }

    /**
     * Get blocks for a specific plant
     * Query: SELECT location_code as value, location_code as label FROM fslocation WHERE location_code LIKE :plantCode || '%'
     */
    @GetMapping("/plants/{plantCode}/blocks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BlockOption>> getBlocksByPlant(@PathVariable String plantCode) {
        List<BlockOption> blocks = projectService.getBlocksByPlant(plantCode);
        return ResponseEntity.ok(blocks);
    }

    /**
     * Validate project code
     */
    @GetMapping("/{projectCode}/validate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validateProjectCode(@PathVariable String projectCode) {
        boolean isValid = projectService.isValidProjectCode(projectCode);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate material code for project
     */
    @GetMapping("/{projectCode}/materials/{materialCode}/validate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validateMaterialCode(
            @PathVariable String projectCode, 
            @PathVariable String materialCode) {
        boolean isValid = projectService.isValidMaterialCode(projectCode, materialCode);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate plant code
     */
    @GetMapping("/plants/{plantCode}/validate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validatePlantCode(@PathVariable String plantCode) {
        boolean isValid = projectService.isValidPlantCode(plantCode);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate block code for plant
     */
    @GetMapping("/plants/{plantCode}/blocks/{blockCode}/validate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validateBlockCode(
            @PathVariable String plantCode, 
            @PathVariable String blockCode) {
        boolean isValid = projectService.isValidBlockCode(plantCode, blockCode);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active questionnaire templates
     */
    @GetMapping("/questionnaire/templates")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuestionnaireTemplateDto>> getQuestionnaireTemplates() {
        List<QuestionnaireTemplateDto> templates = projectService.getQuestionnaireTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get questionnaire templates organized by steps
     */
    @GetMapping("/questionnaire/steps")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuestionnaireStepDto>> getQuestionnaireSteps() {
        List<QuestionnaireStepDto> steps = projectService.getQuestionnaireSteps();
        return ResponseEntity.ok(steps);
    }

    /**
     * Get questionnaire templates for a specific step
     */
    @GetMapping("/questionnaire/steps/{stepNumber}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuestionnaireTemplateDto>> getQuestionnaireTemplatesByStep(
            @PathVariable Integer stepNumber) {
        List<QuestionnaireTemplateDto> templates = projectService.getQuestionnaireTemplatesByStep(stepNumber);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get questionnaire templates by category
     */
    @GetMapping("/questionnaire/categories/{category}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuestionnaireTemplateDto>> getQuestionnaireTemplatesByCategory(
            @PathVariable String category) {
        List<QuestionnaireTemplateDto> templates = projectService.getQuestionnaireTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get questionnaire template by question ID
     */
    @GetMapping("/questionnaire/questions/{questionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuestionnaireTemplateDto> getQuestionnaireTemplateByQuestionId(
            @PathVariable String questionId) {
        QuestionnaireTemplateDto template = projectService.getQuestionnaireTemplateByQuestionId(questionId);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all distinct step numbers
     */
    @GetMapping("/questionnaire/step-numbers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Integer>> getQuestionnaireStepNumbers() {
        List<Integer> stepNumbers = projectService.getQuestionnaireStepNumbers();
        return ResponseEntity.ok(stepNumbers);
    }

    /**
     * Get all distinct categories
     */
    @GetMapping("/questionnaire/categories")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<String>> getQuestionnaireCategories() {
        List<String> categories = projectService.getQuestionnaireCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Clear all caches
     */
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearCache() {
        projectService.clearCache();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All caches cleared successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Clear specific cache
     */
    @PostMapping("/cache/clear/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearSpecificCache(@PathVariable String cacheName) {
        projectService.clearCache(cacheName);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache '" + cacheName + "' cleared successfully");
        return ResponseEntity.ok(response);
    }

    // Exception handlers
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid argument");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Internal server error");
        errorResponse.put("message", "An error occurred while processing the request");
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}