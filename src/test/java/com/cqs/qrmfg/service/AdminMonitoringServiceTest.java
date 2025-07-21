package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.QuerySlaReportDto;
import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.dto.WorkflowMonitoringDto;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.DashboardRepository;
import com.cqs.qrmfg.repository.MaterialWorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import com.cqs.qrmfg.service.impl.AdminMonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMonitoringServiceTest {

    @Mock
    private MaterialWorkflowRepository materialWorkflowRepository;

    @Mock
    private QueryRepository queryRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminMonitoringServiceImpl adminMonitoringService;

    private MaterialWorkflow testWorkflow;
    private Query testQuery;

    @BeforeEach
    void setUp() {
        testWorkflow = new MaterialWorkflow();
        testWorkflow.setId(1L);
        testWorkflow.setMaterialId("MAT001");
        testWorkflow.setState(WorkflowState.PLANT_PENDING);
        testWorkflow.setAssignedPlant("Plant A");
        testWorkflow.setInitiatedBy("jvc_user");
        testWorkflow.setCreatedAt(LocalDateTime.now().minusDays(2));
        testWorkflow.setLastModified(LocalDateTime.now().minusDays(1));

        testQuery = new Query();
        testQuery.setId(1L);
        testQuery.setWorkflow(testWorkflow);
        testQuery.setQuestion("Test question");
        testQuery.setAssignedTeam(QueryTeam.CQS);
        testQuery.setStatus(QueryStatus.OPEN);
        testQuery.setRaisedBy("plant_user");
        testQuery.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testGetWorkflowMonitoringDashboard() {
        // Arrange
        when(materialWorkflowRepository.count()).thenReturn(10L);
        when(materialWorkflowRepository.countByStateNot(WorkflowState.COMPLETED)).thenReturn(7L);
        when(materialWorkflowRepository.countByState(WorkflowState.COMPLETED)).thenReturn(3L);
        when(materialWorkflowRepository.countByStateNotAndCreatedAtBefore(eq(WorkflowState.COMPLETED), any(LocalDateTime.class))).thenReturn(2L);
        when(queryRepository.countDistinctWorkflowIdByStatus(QueryStatus.OPEN)).thenReturn(3L);
        when(materialWorkflowRepository.countByAssignedPlantGrouped()).thenReturn(Arrays.asList(
            new Object[]{"Plant A", 5L},
            new Object[]{"Plant B", 3L}
        ));
        when(materialWorkflowRepository.countByCreatedAtAfterGroupByDay(any(LocalDateTime.class))).thenReturn(Arrays.asList(
            new Object[]{"2024-01-01", 2L},
            new Object[]{"2024-01-02", 3L}
        ));
        when(materialWorkflowRepository.calculateAverageCompletionTimeHours()).thenReturn(48.5);
        when(queryRepository.count()).thenReturn(15L);
        when(queryRepository.countByStatus(QueryStatus.OPEN)).thenReturn(5L);
        when(queryRepository.countByStatusAndCreatedAtBefore(eq(QueryStatus.OPEN), any(LocalDateTime.class))).thenReturn(2L);

        // Act
        WorkflowMonitoringDto result = adminMonitoringService.getWorkflowMonitoringDashboard();

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getTotalWorkflows());
        assertEquals(7L, result.getActiveWorkflows());
        assertEquals(3L, result.getCompletedWorkflows());
        assertEquals(2L, result.getOverdueWorkflows());
        assertEquals(3L, result.getWorkflowsWithOpenQueries());
        assertEquals(48.5, result.getAverageCompletionTimeHours());
        assertEquals(15L, result.getTotalQueries());
        assertEquals(5L, result.getOpenQueries());
        assertEquals(2L, result.getOverdueQueries());
        
        assertNotNull(result.getWorkflowsByPlant());
        assertEquals(2, result.getWorkflowsByPlant().size());
        assertEquals(5L, result.getWorkflowsByPlant().get("Plant A"));
        assertEquals(3L, result.getWorkflowsByPlant().get("Plant B"));
        
        assertNotNull(result.getRecentActivity());
        assertEquals(2, result.getRecentActivity().size());
    }

    @Test
    void testGetWorkflowStatusDistribution() {
        // Arrange
        when(materialWorkflowRepository.countByState(WorkflowState.JVC_PENDING)).thenReturn(2L);
        when(materialWorkflowRepository.countByState(WorkflowState.PLANT_PENDING)).thenReturn(3L);
        when(materialWorkflowRepository.countByState(WorkflowState.CQS_PENDING)).thenReturn(1L);
        when(materialWorkflowRepository.countByState(WorkflowState.TECH_PENDING)).thenReturn(1L);
        when(materialWorkflowRepository.countByState(WorkflowState.COMPLETED)).thenReturn(3L);

        // Act
        Map<String, Long> result = adminMonitoringService.getWorkflowStatusDistribution();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(2L, result.get("JVC_PENDING"));
        assertEquals(3L, result.get("PLANT_PENDING"));
        assertEquals(1L, result.get("CQS_PENDING"));
        assertEquals(1L, result.get("TECH_PENDING"));
        assertEquals(3L, result.get("COMPLETED"));
    }

    @Test
    void testGetQuerySlaReport() {
        // Arrange
        List<Query> mockQueries = Arrays.asList(testQuery);
        when(queryRepository.findAll()).thenReturn(mockQueries);

        // Act
        QuerySlaReportDto result = adminMonitoringService.getQuerySlaReport(null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTotalQueries());
        assertNotNull(result.getTotalQueriesByTeam());
        assertNotNull(result.getAverageResolutionTimesByTeam());
        assertNotNull(result.getSlaComplianceByTeam());
    }

    @Test
    void testGetWorkflowBottlenecks() {
        // Arrange
        when(materialWorkflowRepository.calculateAverageTimeInEachStateGrouped()).thenReturn(Arrays.asList(
            new Object[]{"PLANT_PENDING", 24.5},
            new Object[]{"CQS_PENDING", 12.0}
        ));
        when(materialWorkflowRepository.countOverdueWorkflowsByStateGrouped()).thenReturn(Arrays.asList(
            new Object[]{"PLANT_PENDING", 2L},
            new Object[]{"CQS_PENDING", 1L}
        ));
        when(queryRepository.countOpenQueriesByTeamGrouped()).thenReturn(Arrays.asList(
            new Object[]{"CQS", 3L},
            new Object[]{"TECH", 2L}
        ));
        when(materialWorkflowRepository.countDelayedWorkflowsByPlantGrouped()).thenReturn(Arrays.asList(
            new Object[]{"Plant A", 2L},
            new Object[]{"Plant B", 1L}
        ));

        // Act
        Map<String, Object> result = adminMonitoringService.getWorkflowBottlenecks();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("averageTimeInState"));
        assertTrue(result.containsKey("overdueByState"));
        assertTrue(result.containsKey("openQueriesByTeam"));
        assertTrue(result.containsKey("delayedByPlant"));

        @SuppressWarnings("unchecked")
        Map<String, Double> avgTimeInState = (Map<String, Double>) result.get("averageTimeInState");
        assertEquals(24.5, avgTimeInState.get("PLANT_PENDING"));
        assertEquals(12.0, avgTimeInState.get("CQS_PENDING"));
    }

    @Test
    void testExportAuditLogsAsCsv() {
        // Arrange
        List<Map<String, Object>> mockAuditLogs = Arrays.asList(
            createMockAuditLog("user1", "CREATE_WORKFLOW", "MaterialWorkflow", "1", "Created workflow"),
            createMockAuditLog("user2", "RESOLVE_QUERY", "Query", "1", "Resolved query")
        );
        when(auditLogService.getFilteredAuditLogs(any(), any(), any(), any())).thenReturn(mockAuditLogs);

        // Act
        byte[] result = adminMonitoringService.exportAuditLogsAsCsv(null, null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Timestamp,User,Action,Entity Type,Entity ID,Details"));
        assertTrue(csvContent.contains("CREATE_WORKFLOW"));
        assertTrue(csvContent.contains("RESOLVE_QUERY"));
    }

    @Test
    void testExportWorkflowReportAsCsv() {
        // Arrange
        List<MaterialWorkflow> mockWorkflows = Arrays.asList(testWorkflow);
        when(materialWorkflowRepository.findAll()).thenReturn(mockWorkflows);

        // Act
        byte[] result = adminMonitoringService.exportWorkflowReportAsCsv(null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Material ID,State,Assigned Plant,Initiated By"));
        assertTrue(csvContent.contains("MAT001"));
        assertTrue(csvContent.contains("PLANT_PENDING"));
        assertTrue(csvContent.contains("Plant A"));
    }

    @Test
    void testGetUserRoleAssignments() {
        // Arrange
        List<UserRoleAssignmentDto> mockAssignments = Arrays.asList(
            new UserRoleAssignmentDto(1L, "user1", "user1@example.com", Arrays.asList(), Arrays.asList())
        );
        when(userService.getAllUserRoleAssignments()).thenReturn(mockAssignments);

        // Act
        List<UserRoleAssignmentDto> result = adminMonitoringService.getUserRoleAssignments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user1@example.com", result.get(0).getEmail());
    }

    @Test
    void testUpdateUserRoles() {
        // Arrange
        Long userId = 1L;
        List<Long> roleIds = Arrays.asList(1L, 2L);
        UserRoleAssignmentDto mockUpdatedUser = new UserRoleAssignmentDto(userId, "user1", "user1@example.com", Arrays.asList(), Arrays.asList());
        when(userService.updateUserRoles(userId, roleIds)).thenReturn(mockUpdatedUser);

        // Act
        UserRoleAssignmentDto result = adminMonitoringService.updateUserRoles(userId, roleIds);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("user1", result.getUsername());
        verify(userService).updateUserRoles(userId, roleIds);
    }

    private Map<String, Object> createMockAuditLog(String user, String action, String entityType, String entityId, String details) {
        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", LocalDateTime.now());
        log.put("user", user);
        log.put("action", action);
        log.put("entityType", entityType);
        log.put("entityId", entityId);
        log.put("details", details);
        return log;
    }
}