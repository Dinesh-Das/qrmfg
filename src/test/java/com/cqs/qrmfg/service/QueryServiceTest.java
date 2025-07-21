package com.cqs.qrmfg.service;

import com.cqs.qrmfg.exception.QueryAlreadyResolvedException;
import com.cqs.qrmfg.exception.QueryException;
import com.cqs.qrmfg.exception.QueryNotFoundException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.MaterialWorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import com.cqs.qrmfg.service.impl.QueryServiceImpl;
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
public class QueryServiceTest {

    @Mock
    private QueryRepository queryRepository;

    @Mock
    private MaterialWorkflowRepository workflowRepository;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private QueryServiceImpl queryService;

    private MaterialWorkflow testWorkflow;
    private Query testQuery;

    @BeforeEach
    void setUp() {
        testWorkflow = new MaterialWorkflow("CHEM-001", "jvc.user", "Plant A");
        testWorkflow.setId(1L);
        testWorkflow.setState(WorkflowState.PLANT_PENDING);

        testQuery = new Query(testWorkflow, "What is the flash point?", 5, "flashPoint", 
                             QueryTeam.CQS, "plant.user");
        testQuery.setId(1L);
    }

    @Test
    void testCreateQuery_Success() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        Query result = queryService.createQuery(1L, "What is the flash point?", 5, "flashPoint", 
                                              QueryTeam.CQS, "plant.user");

        // Then
        assertNotNull(result);
        assertEquals("What is the flash point?", result.getQuestion());
        assertEquals(QueryTeam.CQS, result.getAssignedTeam());
        assertEquals("plant.user", result.getRaisedBy());
        
        verify(workflowRepository, times(2)).findById(1L); // Called in createQuery and validateQueryCreation
        verify(queryRepository).save(any(Query.class));
        verify(workflowService).transitionToState(1L, WorkflowState.CQS_PENDING, "plant.user");
    }

    @Test
    void testCreateQuery_WorkflowNotFound() {
        // Given
        when(workflowRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(WorkflowNotFoundException.class, () -> {
            queryService.createQuery(999L, "Test question", QueryTeam.CQS, "plant.user");
        });

        verify(workflowRepository).findById(999L);
        verify(queryRepository, never()).save(any(Query.class));
        verify(workflowService, never()).transitionToState(anyLong(), any(WorkflowState.class), anyString());
    }

    @Test
    void testCreateQuery_InvalidWorkflowState() {
        // Given
        testWorkflow.setState(WorkflowState.COMPLETED);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When & Then
        assertThrows(QueryException.class, () -> {
            queryService.createQuery(1L, "Test question", QueryTeam.CQS, "plant.user");
        });

        verify(workflowRepository, times(2)).findById(1L); // Called in createQuery and validateQueryCreation
        verify(queryRepository, never()).save(any(Query.class));
    }

    @Test
    void testCreateQueryByMaterialId_Success() {
        // Given
        when(workflowRepository.findByMaterialId("CHEM-001")).thenReturn(Optional.of(testWorkflow));
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow)); // For validation
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        Query result = queryService.createQuery("CHEM-001", "Test question", QueryTeam.CQS, "plant.user");

        // Then
        assertNotNull(result);
        verify(workflowRepository).findByMaterialId("CHEM-001");
        verify(workflowRepository, times(2)).findById(1L); // Called in createQuery and validateQueryCreation
        verify(queryRepository).save(any(Query.class));
        verify(workflowService).transitionToState(1L, WorkflowState.CQS_PENDING, "plant.user");
    }

    @Test
    void testResolveQuery_Success() {
        // Given
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);
        when(workflowService.returnFromQueryState(1L, "cqs.user")).thenReturn(testWorkflow);

        // When
        Query result = queryService.resolveQuery(1L, "Flash point is 65°C", "cqs.user");

        // Then
        assertNotNull(result);
        assertEquals(QueryStatus.RESOLVED, testQuery.getStatus());
        assertEquals("Flash point is 65°C", testQuery.getResponse());
        assertEquals("cqs.user", testQuery.getResolvedBy());
        assertNotNull(testQuery.getResolvedAt());
        
        verify(queryRepository).findById(1L);
        verify(queryRepository).save(testQuery);
        verify(workflowService).returnFromQueryState(1L, "cqs.user");
    }

    @Test
    void testResolveQuery_QueryNotFound() {
        // Given
        when(queryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(QueryNotFoundException.class, () -> {
            queryService.resolveQuery(999L, "Test response", "cqs.user");
        });

        verify(queryRepository).findById(999L);
        verify(queryRepository, never()).save(any(Query.class));
    }

    @Test
    void testResolveQuery_AlreadyResolved() {
        // Given
        testQuery.resolve("Previous response", "previous.user");
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));

        // When & Then
        assertThrows(QueryAlreadyResolvedException.class, () -> {
            queryService.resolveQuery(1L, "New response", "cqs.user");
        });

        verify(queryRepository).findById(1L);
        verify(queryRepository, never()).save(any(Query.class));
    }

    @Test
    void testResolveQuery_WithOpenQueriesRemaining() {
        // Given
        Query anotherQuery = new Query(testWorkflow, "Another question", QueryTeam.CQS, "plant.user");
        testWorkflow.getQueries().add(testQuery);
        testWorkflow.getQueries().add(anotherQuery);
        
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        Query result = queryService.resolveQuery(1L, "Flash point is 65°C", "cqs.user");

        // Then
        assertNotNull(result);
        assertEquals(QueryStatus.RESOLVED, testQuery.getStatus());
        
        // Should not return to PLANT_PENDING because there are still open queries
        verify(workflowService, never()).returnFromQueryState(anyLong(), anyString());
    }

    @Test
    void testAssignToTeam_Success() {
        // Given
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        Query result = queryService.assignToTeam(1L, QueryTeam.TECH, "admin.user");

        // Then
        assertNotNull(result);
        assertEquals(QueryTeam.TECH, testQuery.getAssignedTeam());
        assertEquals("admin.user", testQuery.getUpdatedBy());
        
        verify(queryRepository).findById(1L);
        verify(queryRepository).save(testQuery);
        verify(workflowService).transitionToState(1L, WorkflowState.TECH_PENDING, "admin.user");
    }

    @Test
    void testAssignToTeam_ResolvedQuery() {
        // Given
        testQuery.resolve("Already resolved", "cqs.user");
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));

        // When & Then
        assertThrows(QueryException.class, () -> {
            queryService.assignToTeam(1L, QueryTeam.TECH, "admin.user");
        });

        verify(queryRepository).findById(1L);
        verify(queryRepository, never()).save(any(Query.class));
    }

    @Test
    void testUpdatePriority_Success() {
        // Given
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        Query result = queryService.updatePriority(1L, "HIGH", "admin.user");

        // Then
        assertNotNull(result);
        assertEquals("HIGH", testQuery.getPriorityLevel());
        assertEquals("admin.user", testQuery.getUpdatedBy());
        
        verify(queryRepository).findById(1L);
        verify(queryRepository).save(testQuery);
    }

    @Test
    void testUpdateCategory_Success() {
        // Given
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        Query result = queryService.updateCategory(1L, "SAFETY", "admin.user");

        // Then
        assertNotNull(result);
        assertEquals("SAFETY", testQuery.getQueryCategory());
        assertEquals("admin.user", testQuery.getUpdatedBy());
        
        verify(queryRepository).findById(1L);
        verify(queryRepository).save(testQuery);
    }

    @Test
    void testFindByWorkflowId() {
        // Given
        List<Query> queries = Arrays.asList(testQuery);
        when(queryRepository.findByWorkflowId(1L)).thenReturn(queries);

        // When
        List<Query> result = queryService.findByWorkflowId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQuery, result.get(0));
        verify(queryRepository).findByWorkflowId(1L);
    }

    @Test
    void testFindByMaterialId() {
        // Given
        List<Query> queries = Arrays.asList(testQuery);
        when(queryRepository.findByMaterialId("CHEM-001")).thenReturn(queries);

        // When
        List<Query> result = queryService.findByMaterialId("CHEM-001");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQuery, result.get(0));
        verify(queryRepository).findByMaterialId("CHEM-001");
    }

    @Test
    void testFindOpenQueriesForTeam() {
        // Given
        List<Query> queries = Arrays.asList(testQuery);
        when(queryRepository.findTeamInboxQueries(QueryTeam.CQS)).thenReturn(queries);

        // When
        List<Query> result = queryService.findOpenQueriesForTeam(QueryTeam.CQS);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQuery, result.get(0));
        verify(queryRepository).findTeamInboxQueries(QueryTeam.CQS);
    }

    @Test
    void testFindResolvedQueriesForTeam() {
        // Given
        testQuery.resolve("Test response", "cqs.user");
        List<Query> queries = Arrays.asList(testQuery);
        when(queryRepository.findTeamResolvedQueries(QueryTeam.CQS)).thenReturn(queries);

        // When
        List<Query> result = queryService.findResolvedQueriesForTeam(QueryTeam.CQS);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQuery, result.get(0));
        verify(queryRepository).findTeamResolvedQueries(QueryTeam.CQS);
    }

    @Test
    void testFindOverdueQueries() {
        // Given
        List<Query> queries = Arrays.asList(testQuery);
        when(queryRepository.findOverdueQueries()).thenReturn(queries);

        // When
        List<Query> result = queryService.findOverdueQueries();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(queryRepository).findOverdueQueries();
    }

    @Test
    void testGetAverageResolutionTimeHours() {
        // Given
        when(queryRepository.getAverageResolutionTimeHours(QueryTeam.CQS)).thenReturn(24.5);

        // When
        double result = queryService.getAverageResolutionTimeHours(QueryTeam.CQS);

        // Then
        assertEquals(24.5, result, 0.01);
        verify(queryRepository).getAverageResolutionTimeHours(QueryTeam.CQS);
    }

    @Test
    void testGetAverageResolutionTimeHours_NullResult() {
        // Given
        when(queryRepository.getAverageResolutionTimeHours(QueryTeam.CQS)).thenReturn(null);

        // When
        double result = queryService.getAverageResolutionTimeHours(QueryTeam.CQS);

        // Then
        assertEquals(0.0, result, 0.01);
        verify(queryRepository).getAverageResolutionTimeHours(QueryTeam.CQS);
    }

    @Test
    void testCountOpenQueriesByTeam() {
        // Given
        when(queryRepository.countByAssignedTeamAndStatus(QueryTeam.CQS, QueryStatus.OPEN)).thenReturn(5L);

        // When
        long result = queryService.countOpenQueriesByTeam(QueryTeam.CQS);

        // Then
        assertEquals(5L, result);
        verify(queryRepository).countByAssignedTeamAndStatus(QueryTeam.CQS, QueryStatus.OPEN);
    }

    @Test
    void testCountOverdueQueries() {
        // Given
        when(queryRepository.countOverdueQueries()).thenReturn(3L);

        // When
        long result = queryService.countOverdueQueries();

        // Then
        assertEquals(3L, result);
        verify(queryRepository).countOverdueQueries();
    }

    @Test
    void testValidateQueryCreation_Success() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            queryService.validateQueryCreation(1L, QueryTeam.CQS);
        });

        verify(workflowRepository).findById(1L);
    }

    @Test
    void testValidateQueryCreation_WrongState() {
        // Given
        testWorkflow.setState(WorkflowState.COMPLETED);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When & Then
        assertThrows(QueryException.class, () -> {
            queryService.validateQueryCreation(1L, QueryTeam.CQS);
        });

        verify(workflowRepository).findById(1L);
    }

    @Test
    void testValidateQueryCreation_NullTeam() {
        // Given
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        // When & Then
        assertThrows(QueryException.class, () -> {
            queryService.validateQueryCreation(1L, null);
        });

        verify(workflowRepository).findById(1L);
    }

    @Test
    void testValidateQueryResolution_Success() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            queryService.validateQueryResolution(testQuery, "cqs.user");
        });
    }

    @Test
    void testValidateQueryResolution_AlreadyResolved() {
        // Given
        testQuery.resolve("Already resolved", "cqs.user");

        // When & Then
        assertThrows(QueryAlreadyResolvedException.class, () -> {
            queryService.validateQueryResolution(testQuery, "cqs.user");
        });
    }

    @Test
    void testValidateQueryResolution_NullUser() {
        // When & Then
        assertThrows(QueryException.class, () -> {
            queryService.validateQueryResolution(testQuery, null);
        });
    }

    @Test
    void testCanUserResolveQuery_True() {
        // When
        boolean result = queryService.canUserResolveQuery(testQuery, "cqs.user");

        // Then
        assertTrue(result);
    }

    @Test
    void testCanUserResolveQuery_ResolvedQuery() {
        // Given
        testQuery.resolve("Already resolved", "cqs.user");

        // When
        boolean result = queryService.canUserResolveQuery(testQuery, "cqs.user");

        // Then
        assertFalse(result);
    }

    @Test
    void testCanUserResolveQuery_NullUser() {
        // When
        boolean result = queryService.canUserResolveQuery(testQuery, null);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsQueryOverdue_NotOverdue() {
        // When
        boolean result = queryService.isQueryOverdue(testQuery);

        // Then
        assertFalse(result); // Query was just created
    }

    @Test
    void testIsQueryOverdue_Resolved() {
        // Given
        testQuery.resolve("Test response", "cqs.user");

        // When
        boolean result = queryService.isQueryOverdue(testQuery);

        // Then
        assertFalse(result); // Resolved queries are not overdue
    }

    @Test
    void testIsQueryHighPriority_Normal() {
        // When
        boolean result = queryService.isQueryHighPriority(testQuery);

        // Then
        assertFalse(result); // Default priority is NORMAL
    }

    @Test
    void testIsQueryHighPriority_High() {
        // Given
        testQuery.setPriorityLevel("HIGH");

        // When
        boolean result = queryService.isQueryHighPriority(testQuery);

        // Then
        assertTrue(result);
    }

    @Test
    void testHasWorkflowOpenQueries() {
        // Given
        when(queryRepository.hasWorkflowOpenQueries(1L)).thenReturn(true);

        // When
        boolean result = queryService.hasWorkflowOpenQueries(1L);

        // Then
        assertTrue(result);
        verify(queryRepository).hasWorkflowOpenQueries(1L);
    }

    @Test
    void testFindQueriesBlockingWorkflow() {
        // Given
        List<Query> queries = Arrays.asList(testQuery);
        when(queryRepository.findOpenQueriesByWorkflow(1L)).thenReturn(queries);

        // When
        List<Query> result = queryService.findQueriesBlockingWorkflow(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testQuery, result.get(0));
        verify(queryRepository).findOpenQueriesByWorkflow(1L);
    }

    @Test
    void testBulkResolveQueries() {
        // Given
        Query secondQuery = new Query(testWorkflow, "Second question", QueryTeam.CQS, "plant.user");
        secondQuery.setId(2L);
        
        List<Long> queryIds = Arrays.asList(1L, 2L);
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.findById(2L)).thenReturn(Optional.of(secondQuery));
        when(queryRepository.save(any(Query.class))).thenReturn(testQuery);

        // When
        queryService.bulkResolveQueries(queryIds, "Bulk response", "cqs.user");

        // Then
        verify(queryRepository, times(2)).findById(anyLong());
        verify(queryRepository, times(2)).save(any(Query.class));
    }

    @Test
    void testUpdate_Success() {
        // Given
        testQuery.setQuestion("Updated question");
        when(queryRepository.findById(1L)).thenReturn(Optional.of(testQuery));
        when(queryRepository.save(testQuery)).thenReturn(testQuery);

        // When
        Query result = queryService.update(testQuery);

        // Then
        assertNotNull(result);
        assertEquals("Updated question", result.getQuestion());
        verify(queryRepository).findById(1L);
        verify(queryRepository).save(testQuery);
    }

    @Test
    void testUpdate_QueryNotFound() {
        // Given
        testQuery.setId(999L);
        when(queryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(QueryNotFoundException.class, () -> {
            queryService.update(testQuery);
        });

        verify(queryRepository).findById(999L);
        verify(queryRepository, never()).save(any(Query.class));
    }

    @Test
    void testDelete_Success() {
        // Given
        when(queryRepository.existsById(1L)).thenReturn(true);

        // When
        queryService.delete(1L);

        // Then
        verify(queryRepository).existsById(1L);
        verify(queryRepository).deleteById(1L);
    }

    @Test
    void testDelete_QueryNotFound() {
        // Given
        when(queryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(QueryNotFoundException.class, () -> {
            queryService.delete(999L);
        });

        verify(queryRepository).existsById(999L);
        verify(queryRepository, never()).deleteById(any());
    }
}