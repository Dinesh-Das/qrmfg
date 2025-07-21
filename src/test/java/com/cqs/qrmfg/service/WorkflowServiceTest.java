package com.cqs.qrmfg.service;

import com.cqs.qrmfg.exception.InvalidWorkflowStateException;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.MaterialWorkflowRepository;
import com.cqs.qrmfg.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkflowServiceTest {

    @Mock
    private MaterialWorkflowRepository workflowRepository;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private MaterialWorkflow testWorkflow;
    private Query testQuery;

    @BeforeEach
    void setUp() {
        testWorkflow = new MaterialWorkflow("CHEM-001", "jvc.user", "Plant A");
        testWorkflow.setId(1L);
        testWorkflow.setMaterialName("Test Chemical");
        testWorkflow.setMaterialDescription("Test chemical description");

        testQuery = new Query(testWorkflow, "What is the flash point?", QueryTeam.CQS, "plant.user");
        testQuery.setId(1L);
    }

    @Test
    void testInitiateWorkflow_Success() {
        // Given
        when(workflowRepository.existsByMaterialId("CHEM-002")).thenReturn(false);
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.initiateWorkflow("CHEM-002", "Test Material", 
                                                                  "Description", "Plant B", "jvc.user");

        // Then
        assertNotNull(result);
        verify(workflowRepository).existsByMaterialId("CHEM-002");
        verify(workflowRepository).save(any(MaterialWorkflow.class));
    }

    @Test
    void testInitiateWorkflow_AlreadyExists() {
        // Given
        when(workflowRepository.existsByMaterialId("CHEM-001")).thenReturn(true);

        // When & Then
        assertThrows(WorkflowException.class, () -> {
            workflowService.initiateWorkflow("CHEM-001", "Test Material", 
                                           "Description", "Plant A", "jvc.user");
        });

        verify(workflowRepository).existsByMaterialId("CHEM-001");
        verify(workflowRepository, never()).save(any(MaterialWorkflow.class));
    }

    @Test
    void testTransitionToState_Success() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.transitionToState(1L, WorkflowState.PLANT_PENDING, "jvc.user");

        // Then
        assertNotNull(result);
        assertEquals(WorkflowState.PLANT_PENDING, testWorkflow.getState());
        verify(workflowRepository).findById(1L);
        verify(workflowRepository).save(testWorkflow);
    }

    @Test
    void testTransitionToState_WorkflowNotFound() {
        // Given
        when(workflowRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowService.transitionToState(999L, WorkflowState.PLANT_PENDING, "jvc.user");
        });

        verify(workflowRepository).findById(999L);
        verify(workflowRepository, never()).save(any(MaterialWorkflow.class));
    }

    @Test
    void testTransitionToState_InvalidTransition() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When & Then
        assertThrows(InvalidWorkflowStateException.class, () -> {
            workflowService.transitionToState(1L, WorkflowState.COMPLETED, "jvc.user");
        });

        verify(workflowRepository).findById(1L);
        verify(workflowRepository, never()).save(any(MaterialWorkflow.class));
    }

    @Test
    void testExtendToPlant_Success() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.extendToPlant(1L, "jvc.user");

        // Then
        assertNotNull(result);
        assertEquals(WorkflowState.PLANT_PENDING, testWorkflow.getState());
        assertNotNull(testWorkflow.getExtendedAt());
    }

    @Test
    void testCompleteWorkflow_Success() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.completeWorkflow(1L, "plant.user");

        // Then
        assertNotNull(result);
        assertEquals(WorkflowState.COMPLETED, testWorkflow.getState());
        assertNotNull(testWorkflow.getCompletedAt());
    }

    @Test
    void testCompleteWorkflow_WithOpenQueries() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        testWorkflow.getQueries().add(testQuery); // Open query
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When & Then
        assertThrows(WorkflowException.class, () -> {
            workflowService.completeWorkflow(1L, "plant.user");
        });

        verify(workflowRepository).findById(1L);
        verify(workflowRepository, never()).save(any(MaterialWorkflow.class));
    }

    @Test
    void testCompleteWorkflow_WithResolvedQueries() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        testQuery.resolve("Flash point is 65°C", "cqs.user");
        testWorkflow.getQueries().add(testQuery);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.completeWorkflow(1L, "plant.user");

        // Then
        assertNotNull(result);
        assertEquals(WorkflowState.COMPLETED, testWorkflow.getState());
        assertNotNull(testWorkflow.getCompletedAt());
    }

    @Test
    void testMoveToQueryState_Success() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.moveToQueryState(1L, WorkflowState.CQS_PENDING, "plant.user");

        // Then
        assertNotNull(result);
        assertEquals(WorkflowState.CQS_PENDING, testWorkflow.getState());
    }

    @Test
    void testMoveToQueryState_InvalidState() {
        // When & Then
        assertThrows(InvalidWorkflowStateException.class, () -> {
            workflowService.moveToQueryState(1L, WorkflowState.COMPLETED, "plant.user");
        });

        verify(workflowRepository, never()).findById(any());
        verify(workflowRepository, never()).save(any(MaterialWorkflow.class));
    }

    @Test
    void testReturnFromQueryState_Success() {
        // Given
        testWorkflow.setState(WorkflowState.CQS_PENDING);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(any(MaterialWorkflow.class))).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.returnFromQueryState(1L, "cqs.user");

        // Then
        assertNotNull(result);
        assertEquals(WorkflowState.PLANT_PENDING, testWorkflow.getState());
    }

    @Test
    void testCanTransitionTo_ValidTransition() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When
        boolean result = workflowService.canTransitionTo(1L, WorkflowState.PLANT_PENDING);

        // Then
        assertTrue(result);
        verify(workflowRepository).findById(1L);
    }

    @Test
    void testCanTransitionTo_InvalidTransition() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When
        boolean result = workflowService.canTransitionTo(1L, WorkflowState.COMPLETED);

        // Then
        assertFalse(result);
        verify(workflowRepository).findById(1L);
    }

    @Test
    void testFindByState() {
        // Given
        List<MaterialWorkflow> workflows = Arrays.asList(testWorkflow);
        when(workflowRepository.findByState(WorkflowState.JVC_PENDING)).thenReturn(workflows);

        // When
        List<MaterialWorkflow> result = workflowService.findByState(WorkflowState.JVC_PENDING);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testWorkflow, result.get(0));
        verify(workflowRepository).findByState(WorkflowState.JVC_PENDING);
    }

    @Test
    void testFindByAssignedPlant() {
        // Given
        List<MaterialWorkflow> workflows = Arrays.asList(testWorkflow);
        when(workflowRepository.findByAssignedPlant("Plant A")).thenReturn(workflows);

        // When
        List<MaterialWorkflow> result = workflowService.findByAssignedPlant("Plant A");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testWorkflow, result.get(0));
        verify(workflowRepository).findByAssignedPlant("Plant A");
    }

    @Test
    void testFindPendingWorkflows() {
        // Given
        List<MaterialWorkflow> workflows = Arrays.asList(testWorkflow);
        when(workflowRepository.findPendingWorkflows()).thenReturn(workflows);

        // When
        List<MaterialWorkflow> result = workflowService.findPendingWorkflows();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(workflowRepository).findPendingWorkflows();
    }

    @Test
    void testFindOverdueWorkflows() {
        // Given
        List<MaterialWorkflow> workflows = Arrays.asList(testWorkflow);
        when(workflowRepository.findOverdueWorkflows()).thenReturn(workflows);

        // When
        List<MaterialWorkflow> result = workflowService.findOverdueWorkflows();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(workflowRepository).findOverdueWorkflows();
    }

    @Test
    void testCountByState() {
        // Given
        when(workflowRepository.countByState(WorkflowState.JVC_PENDING)).thenReturn(5L);

        // When
        long result = workflowService.countByState(WorkflowState.JVC_PENDING);

        // Then
        assertEquals(5L, result);
        verify(workflowRepository).countByState(WorkflowState.JVC_PENDING);
    }

    @Test
    void testCountOverdueWorkflows() {
        // Given
        when(workflowRepository.countOverdueWorkflows()).thenReturn(3L);

        // When
        long result = workflowService.countOverdueWorkflows();

        // Then
        assertEquals(3L, result);
        verify(workflowRepository).countOverdueWorkflows();
    }

    @Test
    void testIsWorkflowReadyForCompletion_Ready() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When
        boolean result = workflowService.isWorkflowReadyForCompletion(1L);

        // Then
        assertTrue(result);
        verify(workflowRepository).findById(1L);
    }

    @Test
    void testIsWorkflowReadyForCompletion_NotReady() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        testWorkflow.getQueries().add(testQuery); // Open query
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When
        boolean result = workflowService.isWorkflowReadyForCompletion(1L);

        // Then
        assertFalse(result);
        verify(workflowRepository).findById(1L);
    }

    @Test
    void testValidateStateTransition_Valid() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            workflowService.validateStateTransition(testWorkflow, WorkflowState.PLANT_PENDING);
        });
    }

    @Test
    void testValidateStateTransition_Invalid() {
        // When & Then
        assertThrows(InvalidWorkflowStateException.class, () -> {
            workflowService.validateStateTransition(testWorkflow, WorkflowState.COMPLETED);
        });
    }

    @Test
    void testValidateWorkflowCompletion_Valid() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            workflowService.validateWorkflowCompletion(testWorkflow);
        });
    }

    @Test
    void testValidateWorkflowCompletion_WithOpenQueries() {
        // Given
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        testWorkflow.getQueries().add(testQuery); // Open query

        // When & Then
        assertThrows(WorkflowException.class, () -> {
            workflowService.validateWorkflowCompletion(testWorkflow);
        });
    }

    @Test
    void testValidateWorkflowCompletion_WrongState() {
        // Given
        testWorkflow.setState(WorkflowState.CQS_PENDING);

        // When & Then
        assertThrows(InvalidWorkflowStateException.class, () -> {
            workflowService.validateWorkflowCompletion(testWorkflow);
        });
    }

    @Test
    void testFindByMaterialId_Found() {
        // Given
        when(workflowRepository.findByMaterialId("CHEM-001")).thenReturn(Optional.of(testWorkflow));

        // When
        Optional<MaterialWorkflow> result = workflowService.findByMaterialId("CHEM-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testWorkflow, result.get());
        verify(workflowRepository).findByMaterialId("CHEM-001");
    }

    @Test
    void testFindByMaterialId_NotFound() {
        // Given
        when(workflowRepository.findByMaterialId("NONEXISTENT")).thenReturn(Optional.empty());

        // When
        Optional<MaterialWorkflow> result = workflowService.findByMaterialId("NONEXISTENT");

        // Then
        assertFalse(result.isPresent());
        verify(workflowRepository).findByMaterialId("NONEXISTENT");
    }

    @Test
    void testUpdate_Success() {
        // Given
        testWorkflow.setMaterialName("Updated Name");
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.save(testWorkflow)).thenReturn(testWorkflow);

        // When
        MaterialWorkflow result = workflowService.update(testWorkflow);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", result.getMaterialName());
        verify(workflowRepository).findById(1L);
        verify(workflowRepository).save(testWorkflow);
    }

    @Test
    void testUpdate_WorkflowNotFound() {
        // Given
        testWorkflow.setId(999L);
        when(workflowRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowService.update(testWorkflow);
        });

        verify(workflowRepository).findById(999L);
        verify(workflowRepository, never()).save(any(MaterialWorkflow.class));
    }

    @Test
    void testDelete_Success() {
        // Given
        when(workflowRepository.existsById(1L)).thenReturn(true);

        // When
        workflowService.delete(1L);

        // Then
        verify(workflowRepository).existsById(1L);
        verify(workflowRepository).deleteById(1L);
    }

    @Test
    void testDelete_WorkflowNotFound() {
        // Given
        when(workflowRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowService.delete(999L);
        });

        verify(workflowRepository).existsById(999L);
        verify(workflowRepository, never()).deleteById(any());
    }
}