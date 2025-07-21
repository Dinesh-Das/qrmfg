package com.cqs.qrmfg.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

public class WorkflowEntitiesTest {

    private MaterialWorkflow workflow;
    private Query query;
    private QuestionnaireResponse response;

    @BeforeEach
    void setUp() {
        workflow = new MaterialWorkflow("CHEM-001", "jvc.user");
        workflow.setMaterialName("Test Chemical");
        workflow.setAssignedPlant("Plant A");
    }

    @Test
    void testMaterialWorkflowCreation() {
        assertNotNull(workflow);
        assertEquals("CHEM-001", workflow.getMaterialId());
        assertEquals("jvc.user", workflow.getInitiatedBy());
        assertEquals(WorkflowState.JVC_PENDING, workflow.getState());
        assertEquals("Plant A", workflow.getAssignedPlant());
        assertNotNull(workflow.getCreatedAt());
        assertNotNull(workflow.getLastModified());
    }

    @Test
    void testWorkflowStateTransitions() {
        // Test valid transition from JVC_PENDING to PLANT_PENDING
        assertTrue(workflow.canTransitionTo(WorkflowState.PLANT_PENDING));
        workflow.transitionTo(WorkflowState.PLANT_PENDING, "jvc.user");
        assertEquals(WorkflowState.PLANT_PENDING, workflow.getState());
        assertNotNull(workflow.getExtendedAt());

        // Test valid transition from PLANT_PENDING to CQS_PENDING
        assertTrue(workflow.canTransitionTo(WorkflowState.CQS_PENDING));
        workflow.transitionTo(WorkflowState.CQS_PENDING, "plant.user");
        assertEquals(WorkflowState.CQS_PENDING, workflow.getState());

        // Test transition back to PLANT_PENDING
        assertTrue(workflow.canTransitionTo(WorkflowState.PLANT_PENDING));
        workflow.transitionTo(WorkflowState.PLANT_PENDING, "cqs.user");
        assertEquals(WorkflowState.PLANT_PENDING, workflow.getState());

        // Test completion
        assertTrue(workflow.canTransitionTo(WorkflowState.COMPLETED));
        workflow.transitionTo(WorkflowState.COMPLETED, "plant.user");
        assertEquals(WorkflowState.COMPLETED, workflow.getState());
        assertNotNull(workflow.getCompletedAt());

        // Test that completed workflow cannot transition further
        assertFalse(workflow.canTransitionTo(WorkflowState.PLANT_PENDING));
    }

    @Test
    void testInvalidStateTransition() {
        // Test invalid transition from JVC_PENDING to COMPLETED
        assertFalse(workflow.canTransitionTo(WorkflowState.COMPLETED));
        
        assertThrows(IllegalStateException.class, () -> {
            workflow.transitionTo(WorkflowState.COMPLETED, "jvc.user");
        });
    }

    @Test
    void testQueryCreation() {
        query = new Query(workflow, "What is the flash point?", 5, "flashPoint", QueryTeam.CQS, "plant.user");
        
        assertNotNull(query);
        assertEquals(workflow, query.getWorkflow());
        assertEquals("What is the flash point?", query.getQuestion());
        assertEquals(Integer.valueOf(5), query.getStepNumber());
        assertEquals("flashPoint", query.getFieldName());
        assertEquals(QueryTeam.CQS, query.getAssignedTeam());
        assertEquals(QueryStatus.OPEN, query.getStatus());
        assertEquals("plant.user", query.getRaisedBy());
        assertNotNull(query.getCreatedAt());
    }

    @Test
    void testQueryResolution() {
        query = new Query(workflow, "What is the flash point?", QueryTeam.CQS, "plant.user");
        
        assertEquals(QueryStatus.OPEN, query.getStatus());
        assertNull(query.getResponse());
        assertNull(query.getResolvedBy());
        assertNull(query.getResolvedAt());

        // Resolve the query
        query.resolve("Flash point is 65°C", "cqs.user");
        
        assertEquals(QueryStatus.RESOLVED, query.getStatus());
        assertEquals("Flash point is 65°C", query.getResponse());
        assertEquals("cqs.user", query.getResolvedBy());
        assertNotNull(query.getResolvedAt());
    }

    @Test
    void testQueryAlreadyResolved() {
        query = new Query(workflow, "Test question", QueryTeam.CQS, "plant.user");
        query.resolve("First response", "cqs.user");
        
        // Try to resolve again
        assertThrows(IllegalStateException.class, () -> {
            query.resolve("Second response", "cqs.user");
        });
    }

    @Test
    void testQuestionnaireResponseCreation() {
        response = new QuestionnaireResponse(workflow, 3, "materialName", "Test Material", "plant.user");
        
        assertNotNull(response);
        assertEquals(workflow, response.getWorkflow());
        assertEquals(Integer.valueOf(3), response.getStepNumber());
        assertEquals("materialName", response.getFieldName());
        assertEquals("Test Material", response.getFieldValue());
        assertEquals("plant.user", response.getModifiedBy());
        assertEquals("TEXT", response.getFieldType());
        assertFalse(response.getIsDraft());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getLastModified());
    }

    @Test
    void testQuestionnaireResponseUpdate() {
        response = new QuestionnaireResponse(workflow, 3, "materialName", "Initial Value", "plant.user");
        LocalDateTime initialModified = response.getLastModified();
        
        // Wait a bit to ensure timestamp difference
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        response.updateValue("Updated Value", "plant.user");
        
        assertEquals("Updated Value", response.getFieldValue());
        assertFalse(response.getIsDraft());
        assertTrue(response.getLastModified().isAfter(initialModified));
    }

    @Test
    void testQuestionnaireResponseDraft() {
        response = new QuestionnaireResponse(workflow, 3, "materialName", "", "plant.user");
        
        response.saveDraft("Draft value", "plant.user");
        
        assertEquals("Draft value", response.getFieldValue());
        assertTrue(response.getIsDraft());
    }

    @Test
    void testQuestionnaireResponseValidation() {
        response = new QuestionnaireResponse(workflow, 3, "requiredField", "", "plant.user");
        response.setIsRequired(true);
        
        assertTrue(response.isEmpty());
        assertTrue(response.isRequiredAndEmpty());
        assertTrue(response.isValid()); // Initially valid
        
        response.markInvalid("This field is required");
        assertFalse(response.isValid());
        assertEquals("This field is required", response.getValidationMessage());
        
        response.updateValue("Some value", "plant.user");
        assertFalse(response.isEmpty());
        assertFalse(response.isRequiredAndEmpty());
        
        response.markValid();
        assertTrue(response.isValid());
        assertNull(response.getValidationMessage());
    }

    @Test
    void testWorkflowBusinessLogic() {
        // Test days pending calculation
        assertTrue(workflow.getDaysPending() >= 0);
        
        // Test query counts
        assertEquals(0, workflow.getOpenQueriesCount());
        assertEquals(0, workflow.getTotalQueriesCount());
        assertFalse(workflow.hasOpenQueries());
        
        // Add a query
        Query testQuery = new Query(workflow, "Test question", QueryTeam.CQS, "plant.user");
        workflow.getQueries().add(testQuery);
        
        assertEquals(1, workflow.getOpenQueriesCount());
        assertEquals(1, workflow.getTotalQueriesCount());
        assertTrue(workflow.hasOpenQueries());
        
        // Resolve the query
        testQuery.resolve("Test response", "cqs.user");
        
        assertEquals(0, workflow.getOpenQueriesCount());
        assertEquals(1, workflow.getTotalQueriesCount());
        assertFalse(workflow.hasOpenQueries());
    }

    @Test
    void testEnumMethods() {
        // Test WorkflowState enum methods
        assertTrue(WorkflowState.CQS_PENDING.isQueryState());
        assertTrue(WorkflowState.TECH_PENDING.isQueryState());
        assertFalse(WorkflowState.PLANT_PENDING.isQueryState());
        
        assertTrue(WorkflowState.COMPLETED.isTerminalState());
        assertFalse(WorkflowState.PLANT_PENDING.isTerminalState());
        
        // Test QueryTeam enum methods
        assertEquals(WorkflowState.CQS_PENDING, QueryTeam.CQS.getCorrespondingWorkflowState());
        assertEquals(WorkflowState.TECH_PENDING, QueryTeam.TECH.getCorrespondingWorkflowState());
        
        // Test QueryStatus enum methods
        assertTrue(QueryStatus.RESOLVED.isResolved());
        assertFalse(QueryStatus.OPEN.isResolved());
    }
}