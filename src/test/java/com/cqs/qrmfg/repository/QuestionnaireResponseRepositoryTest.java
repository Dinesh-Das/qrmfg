package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.QuestionnaireResponse;
import com.cqs.qrmfg.model.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public class QuestionnaireResponseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QuestionnaireResponseRepository responseRepository;

    private MaterialWorkflow testWorkflow1;
    private MaterialWorkflow testWorkflow2;
    private QuestionnaireResponse response1;
    private QuestionnaireResponse response2;
    private QuestionnaireResponse response3;
    private QuestionnaireResponse response4;

    @BeforeEach
    void setUp() {
        // Create test workflows
        testWorkflow1 = new MaterialWorkflow("CHEM-001", "jvc.user", "Plant A");
        testWorkflow1.setMaterialName("Chemical A");
        testWorkflow1.setState(WorkflowState.PLANT_PENDING);

        testWorkflow2 = new MaterialWorkflow("CHEM-002", "jvc.user", "Plant B");
        testWorkflow2.setMaterialName("Chemical B");
        testWorkflow2.setState(WorkflowState.PLANT_PENDING);

        entityManager.persistAndFlush(testWorkflow1);
        entityManager.persistAndFlush(testWorkflow2);

        // Create test responses
        response1 = new QuestionnaireResponse(testWorkflow1, 1, "materialName", "Test Chemical A", "plant.user");
        response1.setFieldType("TEXT");
        response1.setSectionName("Basic Information");
        response1.setDisplayOrder(1);
        response1.setIsRequired(true);

        response2 = new QuestionnaireResponse(testWorkflow1, 2, "flashPoint", "65°C", "plant.user");
        response2.setFieldType("TEXT");
        response2.setSectionName("Safety Data");
        response2.setDisplayOrder(2);
        response2.setIsRequired(true);

        response3 = new QuestionnaireResponse(testWorkflow1, 3, "notes", "", "plant.user");
        response3.setFieldType("TEXTAREA");
        response3.setSectionName("Additional Information");
        response3.setDisplayOrder(3);
        response3.setIsRequired(false);
        response3.setIsDraft(true);

        response4 = new QuestionnaireResponse(testWorkflow2, 1, "materialName", "Test Chemical B", "plant.user2");
        response4.setFieldType("TEXT");
        response4.setSectionName("Basic Information");
        response4.setDisplayOrder(1);
        response4.setIsRequired(true);
        response4.setValidationStatus("INVALID");
        response4.setValidationMessage("Invalid format");

        entityManager.persistAndFlush(response1);
        entityManager.persistAndFlush(response2);
        entityManager.persistAndFlush(response3);
        entityManager.persistAndFlush(response4);
    }

    @Test
    void testFindByWorkflowId() {
        // When
        List<QuestionnaireResponse> workflow1Responses = responseRepository.findByWorkflowId(testWorkflow1.getId());
        List<QuestionnaireResponse> workflow2Responses = responseRepository.findByWorkflowId(testWorkflow2.getId());

        // Then
        assertThat(workflow1Responses).hasSize(3);
        assertThat(workflow2Responses).hasSize(1);
    }

    @Test
    void testFindByMaterialId() {
        // When
        List<QuestionnaireResponse> chem001Responses = responseRepository.findByMaterialId("CHEM-001");
        List<QuestionnaireResponse> chem002Responses = responseRepository.findByMaterialId("CHEM-002");

        // Then
        assertThat(chem001Responses).hasSize(3);
        assertThat(chem002Responses).hasSize(1);
    }

    @Test
    void testFindByStepNumber() {
        // When
        List<QuestionnaireResponse> step1Responses = responseRepository.findByStepNumber(1);
        List<QuestionnaireResponse> step2Responses = responseRepository.findByStepNumber(2);

        // Then
        assertThat(step1Responses).hasSize(2);
        assertThat(step2Responses).hasSize(1);
        assertThat(step2Responses.get(0).getFieldValue()).isEqualTo("65°C");
    }

    @Test
    void testFindByFieldName() {
        // When
        List<QuestionnaireResponse> materialNameResponses = responseRepository.findByFieldName("materialName");
        List<QuestionnaireResponse> flashPointResponses = responseRepository.findByFieldName("flashPoint");

        // Then
        assertThat(materialNameResponses).hasSize(2);
        assertThat(flashPointResponses).hasSize(1);
    }

    @Test
    void testFindByWorkflowIdAndStepNumber() {
        // When
        List<QuestionnaireResponse> workflow1Step1 = responseRepository.findByWorkflowIdAndStepNumber(testWorkflow1.getId(), 1);

        // Then
        assertThat(workflow1Step1).hasSize(1);
        assertThat(workflow1Step1.get(0).getFieldValue()).isEqualTo("Test Chemical A");
    }

    @Test
    void testFindByWorkflowIdAndStepNumberAndFieldName() {
        // When
        Optional<QuestionnaireResponse> response = responseRepository.findByWorkflowIdAndStepNumberAndFieldName(
            testWorkflow1.getId(), 1, "materialName");

        // Then
        assertThat(response).isPresent();
        assertThat(response.get().getFieldValue()).isEqualTo("Test Chemical A");
    }

    @Test
    void testFindByIsDraft() {
        // When
        List<QuestionnaireResponse> draftResponses = responseRepository.findByIsDraft(true);
        List<QuestionnaireResponse> finalResponses = responseRepository.findByIsDraft(false);

        // Then
        assertThat(draftResponses).hasSize(1);
        assertThat(draftResponses.get(0).getFieldName()).isEqualTo("notes");
        assertThat(finalResponses).hasSize(3);
    }

    @Test
    void testFindByValidationStatus() {
        // When
        List<QuestionnaireResponse> validResponses = responseRepository.findByValidationStatus("VALID");
        List<QuestionnaireResponse> invalidResponses = responseRepository.findByValidationStatus("INVALID");

        // Then
        assertThat(validResponses).hasSize(3);
        assertThat(invalidResponses).hasSize(1);
        assertThat(invalidResponses.get(0).getValidationMessage()).isEqualTo("Invalid format");
    }

    @Test
    void testFindDraftResponsesByWorkflow() {
        // When
        List<QuestionnaireResponse> draftResponses = responseRepository.findDraftResponsesByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(draftResponses).hasSize(1);
        assertThat(draftResponses.get(0).getFieldName()).isEqualTo("notes");
    }

    @Test
    void testFindInvalidResponsesByWorkflow() {
        // When
        List<QuestionnaireResponse> invalidResponses = responseRepository.findInvalidResponsesByWorkflow(testWorkflow2.getId());

        // Then
        assertThat(invalidResponses).hasSize(1);
        assertThat(invalidResponses.get(0).getValidationMessage()).isEqualTo("Invalid format");
    }

    @Test
    void testFindMissingRequiredResponsesByWorkflow() {
        // Given - Create a required response with empty value
        QuestionnaireResponse emptyRequired = new QuestionnaireResponse(testWorkflow1, 4, "requiredField", "", "plant.user");
        emptyRequired.setIsRequired(true);
        entityManager.persistAndFlush(emptyRequired);

        // When
        List<QuestionnaireResponse> missingRequired = responseRepository.findMissingRequiredResponsesByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(missingRequired).hasSize(1);
        assertThat(missingRequired.get(0).getFieldName()).isEqualTo("requiredField");
    }

    @Test
    void testFindBySectionName() {
        // When
        List<QuestionnaireResponse> basicInfoResponses = responseRepository.findBySectionName("Basic Information");
        List<QuestionnaireResponse> safetyDataResponses = responseRepository.findBySectionName("Safety Data");

        // Then
        assertThat(basicInfoResponses).hasSize(2);
        assertThat(safetyDataResponses).hasSize(1);
    }

    @Test
    void testFindByWorkflowIdAndSectionNameOrderByDisplayOrder() {
        // When
        List<QuestionnaireResponse> orderedResponses = responseRepository.findByWorkflowIdAndSectionNameOrderByDisplayOrder(
            testWorkflow1.getId(), "Basic Information");

        // Then
        assertThat(orderedResponses).hasSize(1);
        assertThat(orderedResponses.get(0).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void testCountResponsesByWorkflow() {
        // When
        long workflow1Count = responseRepository.countResponsesByWorkflow(testWorkflow1.getId());
        long workflow2Count = responseRepository.countResponsesByWorkflow(testWorkflow2.getId());

        // Then
        assertThat(workflow1Count).isEqualTo(3);
        assertThat(workflow2Count).isEqualTo(1);
    }

    @Test
    void testCountRequiredResponsesByWorkflow() {
        // When
        long requiredCount = responseRepository.countRequiredResponsesByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(requiredCount).isEqualTo(2); // materialName and flashPoint are required
    }

    @Test
    void testCountCompletedRequiredResponsesByWorkflow() {
        // When
        long completedRequiredCount = responseRepository.countCompletedRequiredResponsesByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(completedRequiredCount).isEqualTo(2); // Both required fields have values
    }

    @Test
    void testCountDraftResponsesByWorkflow() {
        // When
        long draftCount = responseRepository.countDraftResponsesByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(draftCount).isEqualTo(1);
    }

    @Test
    void testCountInvalidResponsesByWorkflow() {
        // When
        long invalidCount = responseRepository.countInvalidResponsesByWorkflow(testWorkflow2.getId());

        // Then
        assertThat(invalidCount).isEqualTo(1);
    }

    @Test
    void testGetCompletionPercentageByWorkflow() {
        // When
        Double completionPercentage = responseRepository.getCompletionPercentageByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(completionPercentage).isNotNull();
        // 2 out of 3 responses have values (notes is empty), so should be around 66.67%
        assertThat(completionPercentage).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void testFindByFieldType() {
        // When
        List<QuestionnaireResponse> textResponses = responseRepository.findByFieldType("TEXT");
        List<QuestionnaireResponse> textareaResponses = responseRepository.findByFieldType("TEXTAREA");

        // Then
        assertThat(textResponses).hasSize(3);
        assertThat(textareaResponses).hasSize(1);
    }

    @Test
    void testFindRecentlyModified() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(1);

        // When
        List<QuestionnaireResponse> recentResponses = responseRepository.findRecentlyModified(cutoffDate);

        // Then
        assertThat(recentResponses).hasSize(4); // All responses were created recently
    }

    @Test
    void testHasWorkflowMissingRequiredResponses() {
        // Given - Create a workflow with missing required response
        QuestionnaireResponse missingRequired = new QuestionnaireResponse(testWorkflow1, 5, "missingField", "", "plant.user");
        missingRequired.setIsRequired(true);
        entityManager.persistAndFlush(missingRequired);

        // When
        boolean hasMissing = responseRepository.hasWorkflowMissingRequiredResponses(testWorkflow1.getId());

        // Then
        assertThat(hasMissing).isTrue();
    }

    @Test
    void testHasWorkflowInvalidResponses() {
        // When
        boolean workflow1HasInvalid = responseRepository.hasWorkflowInvalidResponses(testWorkflow1.getId());
        boolean workflow2HasInvalid = responseRepository.hasWorkflowInvalidResponses(testWorkflow2.getId());

        // Then
        assertThat(workflow1HasInvalid).isFalse();
        assertThat(workflow2HasInvalid).isTrue();
    }

    @Test
    void testFindByModifiedBy() {
        // When
        List<QuestionnaireResponse> plantUserResponses = responseRepository.findByModifiedBy("plant.user");
        List<QuestionnaireResponse> plantUser2Responses = responseRepository.findByModifiedBy("plant.user2");

        // Then
        assertThat(plantUserResponses).hasSize(3);
        assertThat(plantUser2Responses).hasSize(1);
    }

    @Test
    void testSaveAndUpdate() {
        // Given
        QuestionnaireResponse newResponse = new QuestionnaireResponse(testWorkflow1, 6, "newField", "initial value", "plant.user");

        // When - Save
        QuestionnaireResponse saved = responseRepository.save(newResponse);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFieldValue()).isEqualTo("initial value");

        // When - Update
        saved.updateValue("updated value", "plant.user");
        QuestionnaireResponse updated = responseRepository.save(saved);

        // Then
        assertThat(updated.getFieldValue()).isEqualTo("updated value");
        assertThat(updated.getIsDraft()).isFalse();
    }

    @Test
    void testBusinessLogicMethods() {
        // Test isEmpty
        assertThat(response3.isEmpty()).isTrue(); // notes field is empty
        assertThat(response1.isEmpty()).isFalse();

        // Test isValid
        assertThat(response1.isValid()).isTrue();
        assertThat(response4.isValid()).isFalse();

        // Test isRequiredAndEmpty
        assertThat(response3.isRequiredAndEmpty()).isFalse(); // not required
        
        // Create a required empty response for testing
        QuestionnaireResponse requiredEmpty = new QuestionnaireResponse(testWorkflow1, 7, "requiredEmpty", "", "plant.user");
        requiredEmpty.setIsRequired(true);
        assertThat(requiredEmpty.isRequiredAndEmpty()).isTrue();
    }
}