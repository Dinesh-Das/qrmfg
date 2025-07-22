package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.model.WorkflowState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Get workflow counts by state
     * @return Map of state name to count
     */
    public Map<String, Long> getWorkflowCountsByState() {
        String sql = "SELECT workflow_state, COUNT(*) as count FROM qrmfg_material_workflows GROUP BY workflow_state";
        
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            counts.put(rs.getString("workflow_state"), rs.getLong("count"));
            return null;
        });
        
        return counts;
    }
    
    /**
     * Get query counts by team
     * @return Map of team name to count
     */
    public Map<String, Long> getQueryCountsByTeam() {
        String sql = "SELECT assigned_team, COUNT(*) as count FROM qrmfg_queries WHERE query_status = 'OPEN' GROUP BY assigned_team";
        
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            counts.put(rs.getString("assigned_team"), rs.getLong("count"));
            return null;
        });
        
        return counts;
    }
    
    /**
     * Get overdue workflows
     * @param dayThreshold Number of days after which a workflow is considered overdue
     * @return List of overdue workflow summaries
     */
    public List<WorkflowSummaryDto> getOverdueWorkflows(int dayThreshold) {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(SYSDATE - w.created_at) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(SYSDATE - NVL(w.extended_at, w.created_at)) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(SYSDATE - w.last_modified) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM qrmfg_material_workflows w " +
                     "WHERE w.workflow_state != 'COMPLETED' " +
                     "AND ( " +
                     "    (w.workflow_state = 'JVC_PENDING' AND TRUNC(SYSDATE - w.created_at) > ?) " +
                     "    OR (w.workflow_state = 'PLANT_PENDING' AND TRUNC(SYSDATE - NVL(w.extended_at, w.created_at)) > ?) " +
                     "    OR (w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND TRUNC(SYSDATE - w.last_modified) > ?) " +
                     ") " +
                     "ORDER BY days_pending DESC";
        
        return jdbcTemplate.query(sql, 
            new Object[]{dayThreshold, dayThreshold, dayThreshold}, 
            (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get workflows with open queries
     * @return List of workflow summaries with open queries
     */
    public List<WorkflowSummaryDto> getWorkflowsWithOpenQueries() {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(SYSDATE - w.created_at) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(SYSDATE - NVL(w.extended_at, w.created_at)) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(SYSDATE - w.last_modified) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM qrmfg_material_workflows w " +
                     "WHERE EXISTS (SELECT 1 FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') " +
                     "ORDER BY open_queries DESC";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get recent activity across all workflows
     * @param days Number of days to look back
     * @return List of workflow summaries with recent activity
     */
    public List<WorkflowSummaryDto> getRecentActivity(int days) {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(SYSDATE - w.created_at) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(SYSDATE - NVL(w.extended_at, w.created_at)) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(SYSDATE - w.last_modified) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM qrmfg_material_workflows w " +
                     "WHERE w.last_modified >= SYSDATE - ? " +
                     "ORDER BY w.last_modified DESC";
        
        return jdbcTemplate.query(sql, new Object[]{days}, (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get workflows by plant with status summary
     * @param plantName Name of the plant
     * @return List of workflow summaries for the plant
     */
    public List<WorkflowSummaryDto> getWorkflowsByPlant(String plantName) {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(SYSDATE - w.created_at) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(SYSDATE - NVL(w.extended_at, w.created_at)) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(SYSDATE - w.last_modified) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM qrmfg_material_workflows w " +
                     "WHERE w.assigned_plant = ? " +
                     "ORDER BY w.last_modified DESC";
        
        return jdbcTemplate.query(sql, new Object[]{plantName}, (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get dashboard summary statistics
     * @return Map of statistic name to value
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Total workflows
        Integer totalWorkflows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_material_workflows", Integer.class);
        summary.put("totalWorkflows", totalWorkflows);
        
        // Active workflows
        Integer activeWorkflows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED'", Integer.class);
        summary.put("activeWorkflows", activeWorkflows);
        
        // Completed workflows
        Integer completedWorkflows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_material_workflows WHERE workflow_state = 'COMPLETED'", Integer.class);
        summary.put("completedWorkflows", completedWorkflows);
        
        // Overdue workflows
        Integer overdueWorkflows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_material_workflows w " +
            "WHERE w.workflow_state != 'COMPLETED' " +
            "AND ( " +
            "    (w.workflow_state = 'JVC_PENDING' AND TRUNC(SYSDATE - w.created_at) > 3) " +
            "    OR (w.workflow_state = 'PLANT_PENDING' AND TRUNC(SYSDATE - NVL(w.extended_at, w.created_at)) > 3) " +
            "    OR (w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND TRUNC(SYSDATE - w.last_modified) > 3) " +
            ")", Integer.class);
        summary.put("overdueWorkflows", overdueWorkflows);
        
        // Total queries
        Integer totalQueries = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_queries", Integer.class);
        summary.put("totalQueries", totalQueries);
        
        // Open queries
        Integer openQueries = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_queries WHERE query_status = 'OPEN'", Integer.class);
        summary.put("openQueries", openQueries);
        
        // Resolved queries
        Integer resolvedQueries = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_queries WHERE query_status = 'RESOLVED'", Integer.class);
        summary.put("resolvedQueries", resolvedQueries);
        
        // Average resolution time (hours)
        Double avgResolutionTime = jdbcTemplate.queryForObject(
            "SELECT AVG((resolved_at - created_at) * 24) " +
            "FROM qrmfg_queries " +
            "WHERE query_status = 'RESOLVED' AND resolved_at IS NOT NULL", Double.class);
        summary.put("avgResolutionTimeHours", avgResolutionTime != null ? avgResolutionTime : 0);
        
        // Workflows created in last 7 days
        Integer recentWorkflows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_material_workflows WHERE created_at >= SYSDATE - 7", Integer.class);
        summary.put("recentWorkflows", recentWorkflows);
        
        // Workflows completed in last 7 days
        Integer recentCompletions = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qrmfg_material_workflows " +
            "WHERE workflow_state = 'COMPLETED' " +
            "AND completed_at >= SYSDATE - 7", Integer.class);
        summary.put("recentCompletions", recentCompletions);
        
        return summary;
    }
    
    // Helper method to map ResultSet to WorkflowSummaryDto
    private WorkflowSummaryDto mapWorkflowSummary(ResultSet rs) throws SQLException {
        WorkflowSummaryDto dto = new WorkflowSummaryDto();
        dto.setId(rs.getLong("id"));
        dto.setMaterialCode(rs.getString("material_code"));
        dto.setMaterialName(rs.getString("material_name"));
        dto.setCurrentState(WorkflowState.valueOf(rs.getString("workflow_state")));
        dto.setAssignedPlant(rs.getString("assigned_plant"));
        dto.setInitiatedBy(rs.getString("initiated_by"));
        dto.setDaysPending(rs.getInt("days_pending"));
        dto.setTotalQueries(rs.getLong("total_queries"));
        dto.setOpenQueries(rs.getLong("open_queries"));
        dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dto.setLastModified(rs.getTimestamp("last_modified").toLocalDateTime());
        dto.setOverdue(dto.getDaysPending() > 3); // Business rule: overdue after 3 days
        return dto;
    }
}