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
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.materialId = :materialId")
    List<Query> findByMaterialId(@Param("materialId") String materialId);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.materialId = :materialId AND q.status = :status")
    List<Query> findByMaterialIdAndStatus(@Param("materialId") String materialId, @Param("status") QueryStatus status);
    
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
}