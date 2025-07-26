package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.*;
import com.cqs.qrmfg.repository.*;
import com.cqs.qrmfg.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlantQuestionnaireService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private QuestionTemplateRepository questionTemplateRepository;
    
    @Autowired
    private PlantSpecificDataRepository plantSpecificDataRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public void initializePlantQuestionnaire(Workflow workflow, String plantCode, String blockCode) {
        String materialCode = workflow.getMaterialCode();
        
        // Get all questions for this material
        List<Question> masterQuestions = questionRepository.findByMaterialCodeAndIsActiveTrueOrderByOrderIndexAsc(materialCode);
        
        // Create response entries for plant-specific questions
        List<Answer> responses = masterQuestions.stream()
            .filter(q -> shouldCreateResponseForPlant(q))
            .map(q -> createPlantResponse(workflow, q, plantCode, blockCode, materialCode))
            .collect(Collectors.toList());
        
        answerRepository.saveAll(responses);
    }

    private boolean shouldCreateResponseForPlant(Question question) {
        String responsible = question.getResponsible();
        return "Plant".equalsIgnoreCase(responsible) || 
               "All Plants".equalsIgnoreCase(responsible) ||
               "Plant to fill data".equalsIgnoreCase(responsible);
    }

    private Answer createPlantResponse(Workflow workflow, 
                                     Question masterQuestion,
                                     String plantCode, String blockCode, String materialCode) {
        Answer response = new Answer();
        
        response.setWorkflow(workflow);
        response.setStepNumber(masterQuestion.getStepNumber());
        response.setFieldName(masterQuestion.getFieldName());
        response.setFieldType(masterQuestion.getQuestionType());
        response.setSectionName(masterQuestion.getCategory());
        response.setDisplayOrder(masterQuestion.getOrderIndex());
        response.setIsRequired(masterQuestion.getIsRequired());
        response.setPlantCode(plantCode);
        response.setBlockCode(blockCode);
        response.setMaterialCode(materialCode);
        response.setCreatedAt(LocalDateTime.now());
        response.setLastModified(LocalDateTime.now());
        response.setCreatedBy("SYSTEM");
        response.setModifiedBy("SYSTEM");
        response.setIsDraft(true);
        
        return response;
    }

    public List<Answer> getPlantResponses(Long workflowId, String plantCode, String blockCode) {
        return answerRepository.findByPlantCodeAndBlockCodeAndMaterialCode(plantCode, blockCode, "");
    }

    public List<Answer> getPlantResponsesBySection(Long workflowId, String plantCode, 
                                                 String blockCode, String sectionName) {
        // Note: This method needs to be updated based on the new AnswerRepository methods
        return answerRepository.findByPlantCodeAndBlockCodeAndMaterialCode(plantCode, blockCode, "");
    }

    @Transactional
    public void savePlantResponse(Long responseId, String value, String modifiedBy) {
        Answer response = answerRepository.findById(responseId)
            .orElseThrow(() -> new RuntimeException("Response not found: " + responseId));
        
        response.updateValue(value, modifiedBy);
        answerRepository.save(response);
    }

    @Transactional
    public void savePlantResponseDraft(Long responseId, String value, String modifiedBy) {
        Answer response = answerRepository.findById(responseId)
            .orElseThrow(() -> new RuntimeException("Response not found: " + responseId));
        
        response.saveDraft(value, modifiedBy);
        answerRepository.save(response);
    }

    public Map<String, List<Answer>> getPlantResponsesGroupedBySection(Long workflowId, 
                                                                      String plantCode, String blockCode) {
        List<Answer> responses = getPlantResponses(workflowId, plantCode, blockCode);
        return responses.stream()
            .collect(Collectors.groupingBy(Answer::getSectionName));
    }

    public List<String> getAvailableSections(String materialCode) {
        return questionRepository.findDistinctCategoriesByMaterialCode(materialCode);
    }

    public List<Question> getCQSQuestions(String materialCode) {
        return questionRepository.findByMaterialCodeAndResponsible(materialCode, "CQS");
    }

    public List<Question> getPlantQuestions(String materialCode) {
        return questionRepository.findByMaterialCodeAndResponsible(materialCode, "Plant");
    }

    @Transactional
    public void validateAndSubmitPlantResponses(Long workflowId, String plantCode, String blockCode) {
        List<Answer> responses = getPlantResponses(workflowId, plantCode, blockCode);
        
        for (Answer response : responses) {
            if (response.isRequiredAndEmpty()) {
                response.markInvalid("This field is required");
            } else {
                response.markValid();
            }
            response.setIsDraft(false);
        }
        
        answerRepository.saveAll(responses);
    }
    
    /**
     * Get questionnaire template from backend template table
     */
    public QuestionnaireTemplateDto getQuestionnaireTemplate(String materialCode, String plantCode, String templateType) {
        try {
            // Get all active template questions ordered by step and order index
            List<QuestionTemplate> templates = questionTemplateRepository.findByIsActiveTrueOrderByStepNumberAscOrderIndexAsc();
            
            if (templates.isEmpty()) {
                throw new RuntimeException("No questionnaire template found");
            }
            
            // Group templates by step number
            Map<Integer, List<QuestionTemplate>> stepGroups = templates.stream()
                .collect(Collectors.groupingBy(QuestionTemplate::getStepNumber));
            
            // Build template DTO
            QuestionnaireTemplateDto templateDto = new QuestionnaireTemplateDto();
            List<QuestionnaireStepDto> steps = new ArrayList<>();
            
            for (Map.Entry<Integer, List<QuestionTemplate>> entry : stepGroups.entrySet()) {
                Integer stepNumber = entry.getKey();
                List<QuestionTemplate> stepTemplates = entry.getValue();
                
                QuestionnaireStepDto stepDto = new QuestionnaireStepDto();
                stepDto.setStepNumber(stepNumber);
                stepDto.setTitle(getStepTitle(stepNumber, stepTemplates));
                stepDto.setDescription(getStepDescription(stepNumber, stepTemplates));
                
                List<QuestionnaireFieldDto> fields = stepTemplates.stream()
                    .map(this::convertTemplateToField)
                    .collect(Collectors.toList());
                
                stepDto.setFields(fields);
                steps.add(stepDto);
            }
            
            templateDto.setSteps(steps);
            templateDto.setMaterialCode(materialCode);
            templateDto.setPlantCode(plantCode);
            templateDto.setTemplateType(templateType);
            templateDto.setVersion(1);
            templateDto.setCreatedAt(LocalDateTime.now());
            
            return templateDto;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load questionnaire template: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get CQS auto-populated data (mock implementation - replace with actual CQS integration)
     */
    public CqsDataDto getCqsData(String materialCode, String plantCode) {
        try {
            // Mock CQS data - replace with actual CQS service call
            Map<String, Object> cqsData = new HashMap<>();
            
            // Basic Information - CQS auto-populated fields
            cqsData.put("materialName", "Pending IMP");
            cqsData.put("materialType", "Pending IMP");
            cqsData.put("casNumber", "Pending IMP");
            
            // Physical Properties - CQS auto-populated fields
            cqsData.put("physicalState", "Pending IMP");
            cqsData.put("boilingPoint", "Pending IMP");
            cqsData.put("meltingPoint", "Pending IMP");
            
            // Hazard Classification - CQS auto-populated fields
            cqsData.put("hazardCategories", "Pending IMP");
            cqsData.put("signalWord", "Pending IMP");
            cqsData.put("hazardStatements", "Pending IMP");
            
            CqsDataDto cqsDto = new CqsDataDto(materialCode, plantCode, cqsData);
            cqsDto.setSyncStatus("PENDING_IMP");
            cqsDto.setSyncMessage("CQS integration pending implementation");
            cqsDto.setTotalFields(cqsData.size());
            cqsDto.setPopulatedFields(0); // None populated yet
            cqsDto.setCompletionPercentage(0.0);
            
            return cqsDto;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load CQS data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get or create plant-specific data record
     */
    @Transactional
    public PlantSpecificDataDto getOrCreatePlantSpecificData(String plantCode, String materialCode, String blockCode, Long workflowId) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode, blockCode);
            Optional<PlantSpecificData> existing = plantSpecificDataRepository.findById(id);
            
            PlantSpecificData plantData;
            if (existing.isPresent()) {
                plantData = existing.get();
            } else {
                // Create new plant-specific data record
                plantData = new PlantSpecificData(plantCode, materialCode, blockCode);
                plantData.setWorkflowId(workflowId);
                plantData.setCreatedBy("SYSTEM");
                plantData.setUpdatedBy("SYSTEM");
                
                // Initialize with empty CQS and plant data
                plantData.setCqsInputs("{}");
                plantData.setPlantInputs("{}");
                plantData.setCombinedData("{}");
                
                plantData = plantSpecificDataRepository.save(plantData);
            }
            
            return convertToDto(plantData);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get/create plant-specific data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save plant-specific data with composite key
     */
    @Transactional
    public void savePlantSpecificData(PlantSpecificDataDto dataDto, String modifiedBy) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(
                dataDto.getPlantCode(), 
                dataDto.getMaterialCode(), 
                dataDto.getBlockCode()
            );
            
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElse(new PlantSpecificData(dataDto.getPlantCode(), dataDto.getMaterialCode(), dataDto.getBlockCode()));
            
            // Update CQS inputs if provided
            if (dataDto.getCqsInputs() != null) {
                String cqsJson = objectMapper.writeValueAsString(dataDto.getCqsInputs());
                plantData.updateCqsData(cqsJson, modifiedBy);
            }
            
            // Update plant inputs if provided
            if (dataDto.getPlantInputs() != null) {
                String plantJson = objectMapper.writeValueAsString(dataDto.getPlantInputs());
                plantData.updatePlantData(plantJson, modifiedBy);
            }
            
            // Update completion statistics
            if (dataDto.getTotalFields() != null) {
                plantData.updateCompletionStats(
                    dataDto.getTotalFields(),
                    dataDto.getCompletedFields() != null ? dataDto.getCompletedFields() : 0,
                    dataDto.getRequiredFields() != null ? dataDto.getRequiredFields() : 0,
                    dataDto.getCompletedRequiredFields() != null ? dataDto.getCompletedRequiredFields() : 0
                );
            }
            
            plantData.setWorkflowId(dataDto.getWorkflowId());
            plantData.setUpdatedBy(modifiedBy);
            
            plantSpecificDataRepository.save(plantData);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to save plant-specific data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Submit plant questionnaire
     */
    @Transactional
    public void submitPlantQuestionnaire(String plantCode, String materialCode, String blockCode, String submittedBy) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode, blockCode);
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plant-specific data not found"));
            
            plantData.submit(submittedBy);
            plantSpecificDataRepository.save(plantData);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit plant questionnaire: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get plant-specific data by composite key
     */
    public PlantSpecificDataDto getPlantSpecificData(String plantCode, String materialCode, String blockCode) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode, blockCode);
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElse(null);
            
            return plantData != null ? convertToDto(plantData) : null;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get plant-specific data: " + e.getMessage(), e);
        }
    }
    
    // Helper methods
    private String getStepTitle(Integer stepNumber, List<QuestionTemplate> stepTemplates) {
        if (stepTemplates.isEmpty()) {
            return "Step " + stepNumber;
        }
        
        String category = stepTemplates.get(0).getCategory();
        return category != null ? category : "Step " + stepNumber;
    }
    
    private String getStepDescription(Integer stepNumber, List<QuestionTemplate> stepTemplates) {
        // You can enhance this to get description from template or configuration
        Map<Integer, String> stepDescriptions = new HashMap<>();
        stepDescriptions.put(1, "Material identification and basic properties");
        stepDescriptions.put(2, "Physical characteristics and appearance");
        stepDescriptions.put(3, "Safety hazards and classifications");
        stepDescriptions.put(4, "Precautionary and safety information");
        
        return stepDescriptions.getOrDefault(stepNumber, "Step " + stepNumber + " information");
    }
    
    private QuestionnaireFieldDto convertTemplateToField(QuestionTemplate template) {
        QuestionnaireFieldDto field = new QuestionnaireFieldDto();
        field.setName(template.getFieldName());
        field.setLabel(template.getQuestionText());
        field.setType(template.getQuestionType().toLowerCase());
        field.setRequired(template.getIsRequired() != null ? template.getIsRequired() : false);
        field.setHelpText(template.getHelpText());
        field.setOrderIndex(template.getOrderIndex());
        
        // Check if this is a CQS auto-populated field
        boolean isCqsField = template.isForCQS();
        field.setCqsAutoPopulated(isCqsField);
        
        if (isCqsField) {
            field.setDisabled(true);
            field.setCqsValue("Pending IMP");
            field.setPlaceholder("Auto-populated by CQS (Pending Implementation)");
        } else {
            field.setDisabled(false);
            field.setPlaceholder(template.getHelpText());
        }
        
        // Parse options if available
        if (template.hasOptions()) {
            try {
                List<QuestionnaireFieldDto.OptionDto> options = parseOptions(template.getOptions());
                field.setOptions(options);
            } catch (Exception e) {
                // Log error and continue
                System.err.println("Failed to parse options for field " + template.getFieldName() + ": " + e.getMessage());
            }
        }
        
        field.setValidationRules(template.getValidationRules());
        field.setConditionalLogic(template.getConditionalLogic());
        field.setDependsOnField(template.getDependsOnQuestionId());
        
        return field;
    }
    
    private List<QuestionnaireFieldDto.OptionDto> parseOptions(String optionsJson) {
        try {
            if (optionsJson == null || optionsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // Simple parsing - enhance based on your JSON structure
            List<QuestionnaireFieldDto.OptionDto> options = new ArrayList<>();
            
            // For now, assume simple comma-separated values or JSON array
            if (optionsJson.startsWith("[")) {
                // JSON array format
                @SuppressWarnings("unchecked")
                List<Map<String, String>> optionMaps = objectMapper.readValue(optionsJson, List.class);
                for (Map<String, String> optionMap : optionMaps) {
                    options.add(new QuestionnaireFieldDto.OptionDto(
                        optionMap.get("value"), 
                        optionMap.get("label")
                    ));
                }
            } else {
                // Simple comma-separated format
                String[] parts = optionsJson.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    options.add(new QuestionnaireFieldDto.OptionDto(trimmed, trimmed));
                }
            }
            
            return options;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse options: " + optionsJson, e);
        }
    }
    
    private PlantSpecificDataDto convertToDto(PlantSpecificData plantData) {
        try {
            PlantSpecificDataDto dto = new PlantSpecificDataDto();
            dto.setPlantCode(plantData.getPlantCode());
            dto.setMaterialCode(plantData.getMaterialCode());
            dto.setBlockCode(plantData.getBlockCode());
            dto.setCompletionStatus(plantData.getCompletionStatus());
            dto.setCompletionPercentage(plantData.getCompletionPercentage());
            dto.setTotalFields(plantData.getTotalFields());
            dto.setCompletedFields(plantData.getCompletedFields());
            dto.setRequiredFields(plantData.getRequiredFields());
            dto.setCompletedRequiredFields(plantData.getCompletedRequiredFields());
            dto.setCqsSyncStatus(plantData.getCqsSyncStatus());
            dto.setLastCqsSync(plantData.getLastCqsSync());
            dto.setWorkflowId(plantData.getWorkflowId());
            dto.setCreatedAt(plantData.getCreatedAt());
            dto.setUpdatedAt(plantData.getUpdatedAt());
            dto.setCreatedBy(plantData.getCreatedBy());
            dto.setUpdatedBy(plantData.getUpdatedBy());
            dto.setSubmittedAt(plantData.getSubmittedAt());
            dto.setSubmittedBy(plantData.getSubmittedBy());
            dto.setVersion(plantData.getVersion());
            dto.setIsActive(plantData.getIsActive());
            
            // Parse JSON data
            if (plantData.getCqsInputs() != null && !plantData.getCqsInputs().trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cqsInputs = objectMapper.readValue(plantData.getCqsInputs(), Map.class);
                dto.setCqsInputs(cqsInputs);
            }
            
            if (plantData.getPlantInputs() != null && !plantData.getPlantInputs().trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> plantInputs = objectMapper.readValue(plantData.getPlantInputs(), Map.class);
                dto.setPlantInputs(plantInputs);
            }
            
            if (plantData.getCombinedData() != null && !plantData.getCombinedData().trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> combinedData = objectMapper.readValue(plantData.getCombinedData(), Map.class);
                dto.setCombinedData(combinedData);
            }
            
            return dto;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PlantSpecificData to DTO: " + e.getMessage(), e);
        }
    }
}