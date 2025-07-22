package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {
    
    // Basic finders
    List<Query> findByWorkflowId(Long workflowId);
    List<Query> findByStatus(QueryStatus status);
    List<Query> findByAssignedTeam(QueryTeam assignedTeam);
    List<Query> findByRaisedBy(String raisedBy);
    List<Query> findByResolvedBy(String resolvedBy);
    
    // Combined filters
    List<Query> findByWorkflowIdAndStatus(Long workflowId, QueryStatus status);
    List<Query> findByAssignedTeamAndStatus(QueryTeam assignedTeam, QueryStatus status);
    List<Query> findByStatusAndPriorityLevel(QueryStatus status, String priorityLevel);
    
    // Material-based queries
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.materialCode = :materialCode")
    List<Query> findByMaterialCode(@Param("materialCode") String materialCode);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.materialCode = :materialCode AND q.status = :status")
    List<Query> findByMaterialCodeAndStatus(@Param("materialCode") String materialCode, @Param("status") QueryStatus status);
    
    // Time-based queries
    List<Query> findByCreatedAtAfter(LocalDateTime dateTime);
    List<Query> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Query> findByResolvedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Priority and category filters
    List<Query> findByPriorityLevel(String priorityLevel);
    List<Query> findByQueryCategory(String category);
    List<Query> findByAssignedTeamAndPriorityLevel(QueryTeam team, String priorityLevel);
    
    // SLA and overdue queries
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM qrmfg_query WHERE status = 'OPEN' AND ((SYSDATE - created_at) * 24) > :slaHours", nativeQuery = true)
    List<Query> findQueriesOverSLA(@Param("slaHours") int slaHours);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM qrmfg_query WHERE status = 'OPEN' AND (SYSDATE - created_at) > 3", nativeQuery = true)
    List<Query> findOverdueQueries();
    
    // Dashboard queries
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.status = 'OPEN' ORDER BY q.createdAt ASC")
    List<Query> findPendingQueriesForDashboard();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.status = 'OPEN' AND q.priorityLevel IN ('HIGH', 'URGENT') " +
             "ORDER BY q.createdAt ASC")
    List<Query> findHighPriorityQueries();
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM qrmfg_query WHERE status = 'OPEN' AND (priority_level IN ('HIGH', 'URGENT') OR (SYSDATE - created_at) > 2) ORDER BY priority_level DESC, created_at ASC", nativeQuery = true)
    List<Query> findQueriesNeedingAttention();
    
    // Count queries
    long countByStatus(QueryStatus status);
    long countByAssignedTeam(QueryTeam assignedTeam);
    long countByAssignedTeamAndStatus(QueryTeam assignedTeam, QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM qrmfg_query WHERE status = 'OPEN' AND (SYSDATE - created_at) > 3", nativeQuery = true)
    long countOverdueQueries();
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM qrmfg_query WHERE TRUNC(created_at) = TRUNC(SYSDATE)", nativeQuery = true)
    long countQueriesCreatedToday();
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM qrmfg_query WHERE TRUNC(resolved_at) = TRUNC(SYSDATE)", nativeQuery = true)
    long countQueriesResolvedToday();
    
    // Metrics and analytics
    @org.springframework.data.jpa.repository.Query(value = "SELECT AVG((resolved_at - created_at) * 24) FROM qrmfg_query WHERE status = 'RESOLVED' AND assigned_team = :team", nativeQuery = true)
    Double getAverageResolutionTimeHours(@Param("team") String team);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT AVG((resolved_at - created_at) * 24) FROM qrmfg_query WHERE status = 'RESOLVED' AND assigned_team = :team AND resolved_at BETWEEN :start AND :end", nativeQuery = true)
    Double getAverageResolutionTimeHours(@Param("team") String team, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Workflow-specific queries
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.id = :workflowId AND q.status = 'OPEN'")
    List<Query> findOpenQueriesByWorkflow(@Param("workflowId") Long workflowId);
    
    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END FROM Query q " +
             "WHERE q.workflow.id = :workflowId AND q.status = 'OPEN'")
    boolean hasWorkflowOpenQueries(@Param("workflowId") Long workflowId);
    
    // Team inbox queries
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = :team AND q.status = 'OPEN' " +
             "ORDER BY q.priorityLevel DESC, q.createdAt ASC")
    List<Query> findTeamInboxQueries(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = :team AND q.status = 'RESOLVED' " +
             "ORDER BY q.resolvedAt DESC")
    List<Query> findTeamResolvedQueries(@Param("team") QueryTeam team);
    
    // User-specific queries
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.raisedBy = :username ORDER BY q.createdAt DESC")
    List<Query> findQueriesRaisedByUser(@Param("username") String username);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.resolvedBy = :username ORDER BY q.resolvedAt DESC")
    List<Query> findQueriesResolvedByUser(@Param("username") String username);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.raisedBy = :username AND q.status = 'OPEN' " +
             "ORDER BY q.createdAt DESC")
    List<Query> findOpenQueriesRaisedByUser(@Param("username") String username);
    
    // Step and field specific queries
    List<Query> findByStepNumber(Integer stepNumber);
    List<Query> findByFieldName(String fieldName);
    List<Query> findByStepNumberAndFieldName(Integer stepNumber, String fieldName);
    
    // Recent activity
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.createdAt >= :cutoffDate ORDER BY q.createdAt DESC")
    List<Query> findRecentQueries(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.resolvedAt >= :cutoffDate ORDER BY q.resolvedAt DESC")
    List<Query> findRecentlyResolvedQueries(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Admin monitoring specific queries
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT q.workflow.id) FROM Query q WHERE q.status = :status")
    long countDistinctWorkflowIdByStatus(@Param("status") QueryStatus status);
    
    long countByStatusAndCreatedAtBefore(QueryStatus status, LocalDateTime dateTime);
    
    List<Query> findByCreatedAtBefore(LocalDateTime dateTime);
    List<Query> findByStatusAndResolvedAtBetween(QueryStatus status, LocalDateTime start, LocalDateTime end);
    List<Query> findByStatusAndResolvedAtAfter(QueryStatus status, LocalDateTime dateTime);
    List<Query> findByStatusAndResolvedAtBefore(QueryStatus status, LocalDateTime dateTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT q.assignedTeam, COUNT(q) FROM Query q WHERE q.status = 'OPEN' GROUP BY q.assignedTeam")
    List<Object[]> countOpenQueriesByTeamGrouped();
    
    // Team inbox queries including JVC (duplicate methods removed)
    
    // JVC-specific query methods
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = 'JVC' AND q.status = 'OPEN' " +
             "ORDER BY q.createdAt ASC")
    List<Query> findJVCInboxQueries();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = 'JVC' AND q.status = 'RESOLVED' " +
             "ORDER BY q.resolvedAt DESC")
    List<Query> findJVCResolvedQueries();
    
    // Enhanced filtering and search capabilities
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN q.workflow w WHERE " +
           "(:team IS NULL OR q.assignedTeam = :team) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) " +
           "ORDER BY q.createdAt DESC")
    List<Query> findQueriesWithFilters(
        @Param("team") QueryTeam team,
        @Param("status") QueryStatus status,
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("plantCode") String plantCode);
    
    // Search queries by content
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE UPPER(q.question) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
           "OR UPPER(q.response) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY q.createdAt DESC")
    List<Query> searchQueriesByContent(@Param("searchTerm") String searchTerm);

    // Enhanced JVC query support
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN q.workflow w WHERE q.assignedTeam = 'JVC' AND q.status = 'OPEN' AND " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) " +
           "ORDER BY q.createdAt ASC")
    List<Query> findJVCInboxQueriesWithFilters(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("plantCode") String plantCode);

    // Advanced filtering and search capabilities
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN q.workflow w WHERE " +
           "(:team IS NULL OR q.assignedTeam = :team) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) AND " +
           "(:blockId IS NULL OR w.blockId = :blockId) AND " +
           "(:raisedBy IS NULL OR q.raisedBy = :raisedBy) AND " +
           "(:priorityLevel IS NULL OR q.priorityLevel = :priorityLevel) AND " +
           "(:stepNumber IS NULL OR q.stepNumber = :stepNumber) " +
           "ORDER BY q.createdAt DESC")
    List<Query> findQueriesWithAdvancedFilters(
        @Param("team") QueryTeam team,
        @Param("status") QueryStatus status,
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("plantCode") String plantCode,
        @Param("blockId") String blockId,
        @Param("raisedBy") String raisedBy,
        @Param("priorityLevel") String priorityLevel,
        @Param("stepNumber") Integer stepNumber);

    // Query analytics and metrics
    @org.springframework.data.jpa.repository.Query("SELECT q.assignedTeam, q.status, COUNT(q) FROM Query q GROUP BY q.assignedTeam, q.status ORDER BY q.assignedTeam, q.status")
    List<Object[]> getQueryCountByTeamAndStatus();

    @org.springframework.data.jpa.repository.Query("SELECT q.priorityLevel, COUNT(q) FROM Query q WHERE q.status = 'OPEN' GROUP BY q.priorityLevel ORDER BY q.priorityLevel")
    List<Object[]> getOpenQueryCountByPriority();

    @org.springframework.data.jpa.repository.Query("SELECT q.stepNumber, COUNT(q) FROM Query q GROUP BY q.stepNumber ORDER BY q.stepNumber")
    List<Object[]> getQueryCountByStep();

    // Query resolution performance metrics
    @org.springframework.data.jpa.repository.Query(value = "SELECT assigned_team, AVG((resolved_at - created_at) * 24) as avg_hours, COUNT(*) as total_resolved " +
           "FROM qrmfg_queries WHERE query_status = 'RESOLVED' AND resolved_at >= :startDate " +
           "GROUP BY assigned_team ORDER BY avg_hours", nativeQuery = true)
    List<Object[]> getQueryResolutionMetrics(@Param("startDate") LocalDateTime startDate);

    // Bulk operations support
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.id IN :queryIds")
    List<Query> findByIds(@Param("queryIds") List<Long> queryIds);

    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.id IN :workflowIds AND q.status = 'OPEN'")
    List<Query> findOpenQueriesByWorkflowIds(@Param("workflowIds") List<Long> workflowIds);

    // Query escalation support
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM qrmfg_queries WHERE query_status = 'OPEN' AND priority_level IN ('HIGH', 'URGENT') AND (SYSDATE - created_at) > :escalationHours/24", nativeQuery = true)
    List<Query> findQueriesForEscalation(@Param("escalationHours") int escalationHours);

    // Team workload analysis
    @org.springframework.data.jpa.repository.Query(value = "SELECT assigned_team, COUNT(*) as openCount, AVG((SYSDATE - created_at) * 24) as avgAgeHours FROM qrmfg_queries WHERE query_status = 'OPEN' GROUP BY assigned_team", nativeQuery = true)
    List<Object[]> getTeamWorkloadMetrics();
}