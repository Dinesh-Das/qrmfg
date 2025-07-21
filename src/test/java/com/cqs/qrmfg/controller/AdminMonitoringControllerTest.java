package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.QuerySlaReportDto;
import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.dto.WorkflowMonitoringDto;
import com.cqs.qrmfg.service.AdminMonitoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMonitoringController.class)
class AdminMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminMonitoringService adminMonitoringService;

    @Autowired
    private ObjectMapper objectMapper;

    private WorkflowMonitoringDto mockDashboard;
    private QuerySlaReportDto mockSlaReport;
    private List<UserRoleAssignmentDto> mockUserRoles;

    @BeforeEach
    void setUp() {
        // Setup mock dashboard data
        Map<String, Long> workflowsByState = new HashMap<>();
        workflowsByState.put("PLANT_PENDING", 5L);
        workflowsByState.put("COMPLETED", 3L);

        Map<String, Long> workflowsByPlant = new HashMap<>();
        workflowsByPlant.put("Plant A", 4L);
        workflowsByPlant.put("Plant B", 4L);

        Map<String, Long> recentActivity = new HashMap<>();
        recentActivity.put("2024-01-01", 2L);
        recentActivity.put("2024-01-02", 3L);

        mockDashboard = new WorkflowMonitoringDto(
            10L, 7L, 3L, 2L, 3L,
            workflowsByState, workflowsByPlant, recentActivity,
            48.5, 15L, 5L, 2L
        );

        // Setup mock SLA report
        Map<String, Double> avgResolutionTimes = new HashMap<>();
        avgResolutionTimes.put("CQS", 24.5);
        avgResolutionTimes.put("TECH", 18.0);

        Map<String, Long> totalQueries = new HashMap<>();
        totalQueries.put("CQS", 8L);
        totalQueries.put("TECH", 7L);

        Map<String, Long> resolvedQueries = new HashMap<>();
        resolvedQueries.put("CQS", 6L);
        resolvedQueries.put("TECH", 5L);

        Map<String, Long> overdueQueries = new HashMap<>();
        overdueQueries.put("CQS", 2L);
        overdueQueries.put("TECH", 2L);

        Map<String, Double> slaCompliance = new HashMap<>();
        slaCompliance.put("CQS", 75.0);
        slaCompliance.put("TECH", 71.4);

        mockSlaReport = new QuerySlaReportDto(
            avgResolutionTimes, totalQueries, resolvedQueries, overdueQueries, slaCompliance,
            21.2, 15L, 11L, 4L, 73.3
        );

        // Setup mock user roles
        mockUserRoles = Arrays.asList(
            new UserRoleAssignmentDto(1L, "admin", "admin@example.com", Arrays.asList(), Arrays.asList()),
            new UserRoleAssignmentDto(2L, "user1", "user1@example.com", Arrays.asList(), Arrays.asList())
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetWorkflowMonitoringDashboard() throws Exception {
        // Arrange
        when(adminMonitoringService.getWorkflowMonitoringDashboard()).thenReturn(mockDashboard);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalWorkflows").value(10))
                .andExpect(jsonPath("$.activeWorkflows").value(7))
                .andExpect(jsonPath("$.completedWorkflows").value(3))
                .andExpect(jsonPath("$.overdueWorkflows").value(2))
                .andExpect(jsonPath("$.averageCompletionTimeHours").value(48.5))
                .andExpect(jsonPath("$.workflowsByState.PLANT_PENDING").value(5))
                .andExpect(jsonPath("$.workflowsByPlant['Plant A']").value(4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetWorkflowStatusDistribution() throws Exception {
        // Arrange
        Map<String, Long> statusDistribution = new HashMap<>();
        statusDistribution.put("PLANT_PENDING", 5L);
        statusDistribution.put("COMPLETED", 3L);
        statusDistribution.put("CQS_PENDING", 2L);
        
        when(adminMonitoringService.getWorkflowStatusDistribution()).thenReturn(statusDistribution);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/workflow-status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.PLANT_PENDING").value(5))
                .andExpect(jsonPath("$.COMPLETED").value(3))
                .andExpect(jsonPath("$.CQS_PENDING").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetQuerySlaReport() throws Exception {
        // Arrange
        when(adminMonitoringService.getQuerySlaReport(any(), any())).thenReturn(mockSlaReport);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/query-sla"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalQueries").value(15))
                .andExpect(jsonPath("$.totalResolvedQueries").value(11))
                .andExpect(jsonPath("$.overallSlaCompliance").value(73.3))
                .andExpect(jsonPath("$.averageResolutionTimesByTeam.CQS").value(24.5))
                .andExpect(jsonPath("$.slaComplianceByTeam.CQS").value(75.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetWorkflowBottlenecks() throws Exception {
        // Arrange
        Map<String, Object> bottlenecks = new HashMap<>();
        Map<String, Double> avgTimeInState = new HashMap<>();
        avgTimeInState.put("PLANT_PENDING", 24.5);
        avgTimeInState.put("CQS_PENDING", 12.0);
        bottlenecks.put("averageTimeInState", avgTimeInState);

        Map<String, Long> overdueByState = new HashMap<>();
        overdueByState.put("PLANT_PENDING", 2L);
        overdueByState.put("CQS_PENDING", 1L);
        bottlenecks.put("overdueByState", overdueByState);

        when(adminMonitoringService.getWorkflowBottlenecks()).thenReturn(bottlenecks);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/bottlenecks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.averageTimeInState.PLANT_PENDING").value(24.5))
                .andExpect(jsonPath("$.overdueByState.PLANT_PENDING").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportAuditLogs() throws Exception {
        // Arrange
        byte[] csvData = "Timestamp,User,Action,Entity Type,Entity ID,Details\n2024-01-01,user1,CREATE_WORKFLOW,MaterialWorkflow,1,Created workflow".getBytes();
        when(adminMonitoringService.exportAuditLogsAsCsv(any(), any(), any(), any())).thenReturn(csvData);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/audit-logs/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("audit-logs-")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportWorkflowReport() throws Exception {
        // Arrange
        byte[] csvData = "Material ID,State,Assigned Plant,Initiated By\nMAT001,PLANT_PENDING,Plant A,jvc_user".getBytes();
        when(adminMonitoringService.exportWorkflowReportAsCsv(any(), any(), any())).thenReturn(csvData);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/workflows/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("workflow-report-")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUserRoleAssignments() throws Exception {
        // Arrange
        when(adminMonitoringService.getUserRoleAssignments()).thenReturn(mockUserRoles);

        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/user-roles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[1].username").value("user1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserRoles() throws Exception {
        // Arrange
        Long userId = 1L;
        List<Long> roleIds = Arrays.asList(1L, 2L);
        UserRoleAssignmentDto updatedUser = new UserRoleAssignmentDto(userId, "user1", "user1@example.com", Arrays.asList(), Arrays.asList());
        
        when(adminMonitoringService.updateUserRoles(anyLong(), anyList())).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/qrmfg/api/v1/admin/monitoring/user-roles/{userId}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleIds)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAccessDeniedForNonAdminUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/qrmfg/api/v1/admin/monitoring/dashboard"))
                .andExpect(status().isForbidden());
    }
}