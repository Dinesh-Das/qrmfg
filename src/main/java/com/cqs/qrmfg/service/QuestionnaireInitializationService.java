package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionnaireInitializationService {

    @Autowired
    private MaterialQuestionnaireService materialQuestionnaireService;

    @Autowired
    private PlantQuestionnaireService plantService;

    @Autowired
    private WorkflowRepository workflowRepository;

    /**
     * Initialize questionnaire for a new material from template
     * This is what JVC calls when creating a new material workflow
     */
    @Transactional
    public void initializeMaterialQuestionnaire(String materialCode) {
        // Create material-specific questionnaire from template
        materialQuestionnaireService.createMaterialQuestionnaire(materialCode);
    }

    /**
     * Initialize questionnaire for a new material from specific template version
     */
    @Transactional
    public void initializeMaterialQuestionnaireFromVersion(String materialCode, Integer version) {
        // Create material-specific questionnaire from specific template version
        materialQuestionnaireService.createMaterialQuestionnaireFromVersion(materialCode, version);
    }

    /**
     * When JVC sends workflow to plant, initialize plant-specific questionnaire
     */
    @Transactional
    public void initializeWorkflowForPlant(Long workflowId, String plantCode, String blockCode) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
        
        // Create plant-specific questionnaire responses
        plantService.initializePlantQuestionnaire(workflow, plantCode, blockCode);
    }

    /**
     * Initialize questionnaire for multiple plants
     */
    @Transactional
    public void initializeWorkflowForMultiplePlants(Long workflowId, String[] plantCodes, String[] blockCodes) {
        if (plantCodes.length != blockCodes.length) {
            throw new IllegalArgumentException("Plant codes and block codes arrays must have same length");
        }

        for (int i = 0; i < plantCodes.length; i++) {
            initializeWorkflowForPlant(workflowId, plantCodes[i], blockCodes[i]);
        }
    }

    /**
     * Check if material questionnaire exists
     */
    public boolean materialQuestionnaireExists(String materialCode) {
        return materialQuestionnaireService.materialQuestionnaireExists(materialCode);
    }

    /**
     * Get template statistics for admin purposes
     */
    public MaterialQuestionnaireService.TemplateStatistics getTemplateStatistics() {
        return materialQuestionnaireService.getTemplateStatistics();
    }
}