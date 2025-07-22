package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.model.FSLocation;
import com.cqs.qrmfg.model.FSObjectReference;
import com.cqs.qrmfg.model.QRMFGQuestionnaireMaster;
import com.cqs.qrmfg.repository.FSLocationRepository;
import com.cqs.qrmfg.repository.FSObjectReferenceRepository;
import com.cqs.qrmfg.repository.QRMFGQuestionnaireMasterRepository;
import com.cqs.qrmfg.service.ProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FSObjectReferenceRepository fsObjectReferenceRepository;

    @Autowired
    private FSLocationRepository fsLocationRepository;

    @Autowired
    private QRMFGQuestionnaireMasterRepository questionnaireMasterRepository;

    @Override
    @Cacheable(value = "projects", unless = "#result.isEmpty()")
    public List<ProjectOption> getActiveProjects() {
        logger.debug("Fetching active projects from database");
        List<FSObjectReference> projects = fsObjectReferenceRepository.findActiveProjects();
        return projects.stream()
                .map(project -> new ProjectOption(project.getObjectKey(), project.getObjectKey()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "materials", key = "#projectCode", unless = "#result.isEmpty()")
    public List<MaterialOption> getMaterialsByProject(String projectCode) {
        logger.debug("Fetching materials for project: {}", projectCode);
        List<FSObjectReference> materials = fsObjectReferenceRepository.findMaterialsByProject(projectCode);
        return materials.stream()
                .map(material -> new MaterialOption(
                    material.getRObjectKey(), 
                    material.getRObjectDesc() != null ? material.getRObjectDesc() : material.getRObjectKey(),
                    material.getObjectKey()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "plants", unless = "#result.isEmpty()")
    public List<PlantOption> getPlantCodes() {
        logger.debug("Fetching plant codes from database");
        List<FSLocation> plants = fsLocationRepository.findPlantCodes();
        return plants.stream()
                .map(plant -> new PlantOption(plant.getLocationCode(), plant.getLocationCode()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "blocks", key = "#plantCode", unless = "#result.isEmpty()")
    public List<BlockOption> getBlocksByPlant(String plantCode) {
        logger.debug("Fetching blocks for plant: {}", plantCode);
        List<FSLocation> blocks = fsLocationRepository.findBlocksByPlant(plantCode);
        return blocks.stream()
                .map(block -> new BlockOption(block.getLocationCode(), block.getLocationCode(), plantCode))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValidProjectCode(String projectCode) {
        FSObjectReference project = fsObjectReferenceRepository.findProjectByObjectKey(projectCode);
        return project != null;
    }

    @Override
    public boolean isValidMaterialCode(String projectCode, String materialCode) {
        FSObjectReference material = fsObjectReferenceRepository.findMaterialByProjectAndCode(projectCode, materialCode);
        return material != null;
    }

    @Override
    public boolean isValidPlantCode(String plantCode) {
        FSLocation plant = fsLocationRepository.findByLocationCode(plantCode);
        return plant != null;
    }

    @Override
    public boolean isValidBlockCode(String plantCode, String blockCode) {
        FSLocation block = fsLocationRepository.findByLocationCode(blockCode);
        return block != null && blockCode.startsWith(plantCode);
    }

    @Override
    @Cacheable(value = "questionnaireTemplates", unless = "#result.isEmpty()")
    public List<QuestionnaireTemplateDto> getQuestionnaireTemplates() {
        logger.debug("Fetching all questionnaire templates from database");
        List<QRMFGQuestionnaireMaster> templates = questionnaireMasterRepository.findActiveQuestionnaires();
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "questionnaireSteps", unless = "#result.isEmpty()")
    public List<QuestionnaireStepDto> getQuestionnaireSteps() {
        logger.debug("Fetching questionnaire steps from database");
        List<QRMFGQuestionnaireMaster> templates = questionnaireMasterRepository.findActiveQuestionnaires();
        
        Map<Integer, List<QRMFGQuestionnaireMaster>> stepGroups = templates.stream()
                .collect(Collectors.groupingBy(QRMFGQuestionnaireMaster::getStepNumber));
        
        return stepGroups.entrySet().stream()
                .map(entry -> {
                    Integer stepNumber = entry.getKey();
                    List<QRMFGQuestionnaireMaster> stepTemplates = entry.getValue();
                    
                    QuestionnaireStepDto stepDto = new QuestionnaireStepDto();
                    stepDto.setStepNumber(stepNumber);
                    stepDto.setStepTitle("Step " + stepNumber);
                    
                    // Set category from first template in step
                    if (!stepTemplates.isEmpty()) {
                        stepDto.setCategory(stepTemplates.get(0).getCategory());
                    }
                    
                    List<QuestionnaireTemplateDto> questionDtos = stepTemplates.stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                    
                    stepDto.setQuestions(questionDtos);
                    return stepDto;
                })
                .sorted((a, b) -> Integer.compare(a.getStepNumber(), b.getStepNumber()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "questionnaireTemplatesByStep", key = "#stepNumber", unless = "#result.isEmpty()")
    public List<QuestionnaireTemplateDto> getQuestionnaireTemplatesByStep(Integer stepNumber) {
        logger.debug("Fetching questionnaire templates for step: {}", stepNumber);
        List<QRMFGQuestionnaireMaster> templates = questionnaireMasterRepository.findByStepNumber(stepNumber);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "questionnaireTemplatesByCategory", key = "#category", unless = "#result.isEmpty()")
    public List<QuestionnaireTemplateDto> getQuestionnaireTemplatesByCategory(String category) {
        logger.debug("Fetching questionnaire templates for category: {}", category);
        List<QRMFGQuestionnaireMaster> templates = questionnaireMasterRepository.findByCategory(category);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "questionnaireTemplateByQuestionId", key = "#questionId")
    public QuestionnaireTemplateDto getQuestionnaireTemplateByQuestionId(String questionId) {
        logger.debug("Fetching questionnaire template for question ID: {}", questionId);
        QRMFGQuestionnaireMaster template = questionnaireMasterRepository.findByQuestionId(questionId);
        return template != null ? convertToDto(template) : null;
    }

    @Override
    @Cacheable(value = "questionnaireStepNumbers", unless = "#result.isEmpty()")
    public List<Integer> getQuestionnaireStepNumbers() {
        logger.debug("Fetching distinct questionnaire step numbers");
        return questionnaireMasterRepository.findDistinctStepNumbers();
    }

    @Override
    @Cacheable(value = "questionnaireCategories", unless = "#result.isEmpty()")
    public List<String> getQuestionnaireCategories() {
        logger.debug("Fetching distinct questionnaire categories");
        return questionnaireMasterRepository.findDistinctCategories();
    }

    @Override
    @CacheEvict(value = {"projects", "materials", "plants", "blocks", "questionnaireTemplates", 
                        "questionnaireSteps", "questionnaireTemplatesByStep", "questionnaireTemplatesByCategory",
                        "questionnaireTemplateByQuestionId", "questionnaireStepNumbers", "questionnaireCategories"}, 
                allEntries = true)
    public void clearCache() {
        logger.info("Clearing all project service caches");
    }

    @Override
    @CacheEvict(allEntries = true)
    public void clearCache(String cacheName) {
        logger.info("Clearing cache: {}", cacheName);
    }

    private QuestionnaireTemplateDto convertToDto(QRMFGQuestionnaireMaster template) {
        QuestionnaireTemplateDto dto = new QuestionnaireTemplateDto();
        dto.setId(template.getId());
        dto.setSrNo(template.getSrNo());
        dto.setChecklistText(template.getChecklistText());
        dto.setComments(template.getComments());
        dto.setResponsible(template.getResponsible());
        dto.setQuestionId(template.getQuestionId());
        dto.setQuestionText(template.getQuestionText());
        dto.setQuestionType(template.getQuestionType());
        dto.setStepNumber(template.getStepNumber());
        dto.setFieldName(template.getFieldName());
        dto.setIsRequired(template.getIsRequired());
        dto.setValidationRules(template.getValidationRules());
        dto.setConditionalLogic(template.getConditionalLogic());
        dto.setHelpText(template.getHelpText());
        dto.setCategory(template.getCategory());
        dto.setOrderIndex(template.getOrderIndex());
        dto.setIsActive(template.getIsActive());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        dto.setCreatedBy(template.getCreatedBy());
        dto.setUpdatedBy(template.getUpdatedBy());

        // Parse options JSON string to List<String>
        if (template.getOptions() != null && !template.getOptions().trim().isEmpty()) {
            try {
                List<String> options = objectMapper.readValue(template.getOptions(), new TypeReference<List<String>>() {});
                dto.setOptions(options);
            } catch (Exception e) {
                logger.warn("Failed to parse options JSON for question ID {}: {}", template.getQuestionId(), e.getMessage());
                dto.setOptions(new ArrayList<>());
            }
        } else {
            dto.setOptions(new ArrayList<>());
        }

        return dto;
    }
}