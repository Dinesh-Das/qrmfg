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
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * Get all active projects
     * Query: SELECT DISTINCT project_code FROM qrmfg_project_item_master ORDER BY project_code
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProjectOption>> getActiveProjects() {
        List<ProjectOption> projects = projectService.getActiveProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get materials for a specific project
     * Query: SELECT DISTINCT item_code FROM qrmfg_project_item_master WHERE project_code = :projectCode ORDER BY item_code
     */
    @GetMapping("/{projectCode}/materials")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MaterialOption>> getMaterialsByProject(@PathVariable String projectCode) {
        List<MaterialOption> materials = projectService.getMaterialsByProject(projectCode);
        return ResponseEntity.ok(materials);
    }

    /**
     * Get all plant codes
     * Query: SELECT location_code, description FROM qrmfg_location_master ORDER BY location_code
     */
    @GetMapping("/plants")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PlantOption>> getPlantCodes() {
        List<PlantOption> plants = projectService.getPlantCodes();
        return ResponseEntity.ok(plants);
    }

    /**
     * Get blocks for a specific plant
     * Query: SELECT block_id, description FROM qrmfg_block_master ORDER BY block_id
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

    /**
     * Search projects by partial code or name
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProjectOption>> searchProjects(@RequestParam String searchTerm) {
        List<ProjectOption> projects = projectService.searchProjects(searchTerm);
        return ResponseEntity.ok(projects);
    }

    /**
     * Search materials by partial code or description within a project
     */
    @GetMapping("/{projectCode}/materials/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MaterialOption>> searchMaterials(
            @PathVariable String projectCode, 
            @RequestParam String searchTerm) {
        List<MaterialOption> materials = projectService.searchMaterials(projectCode, searchTerm);
        return ResponseEntity.ok(materials);
    }

    /**
     * Search plants by partial code
     */
    @GetMapping("/plants/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PlantOption>> searchPlants(@RequestParam String searchTerm) {
        List<PlantOption> plants = projectService.searchPlants(searchTerm);
        return ResponseEntity.ok(plants);
    }

    /**
     * Search blocks by partial code within a plant
     */
    @GetMapping("/plants/{plantCode}/blocks/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BlockOption>> searchBlocks(
            @PathVariable String plantCode, 
            @RequestParam String searchTerm) {
        List<BlockOption> blocks = projectService.searchBlocks(plantCode, searchTerm);
        return ResponseEntity.ok(blocks);
    }

    /**
     * Get projects with material count for performance insights
     */
    @GetMapping("/with-material-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProjectOption>> getProjectsWithMaterialCount() {
        List<ProjectOption> projects = projectService.getProjectsWithMaterialCount();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get plants with block count for performance insights
     */
    @GetMapping("/plants/with-block-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PlantOption>> getPlantsWithBlockCount() {
        List<PlantOption> plants = projectService.getPlantsWithBlockCount();
        return ResponseEntity.ok(plants);
    }

    /**
     * Bulk validate project codes
     */
    @PostMapping("/validate/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validateProjectCodes(@RequestBody List<String> projectCodes) {
        Map<String, Boolean> results = projectService.validateProjectCodes(projectCodes);
        return ResponseEntity.ok(results);
    }

    /**
     * Bulk validate material codes for projects
     */
    @PostMapping("/materials/validate/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validateMaterialCodes(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> projectCodes = (List<String>) request.get("projectCodes");
        @SuppressWarnings("unchecked")
        List<String> materialCodes = (List<String>) request.get("materialCodes");
        
        Map<String, Boolean> results = projectService.validateMaterialCodes(projectCodes, materialCodes);
        return ResponseEntity.ok(results);
    }

    /**
     * Get cache statistics for monitoring
     */
    @GetMapping("/cache/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> stats = projectService.getCacheStatistics();
        return ResponseEntity.ok(stats);
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