package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {
    
    // Basic finders
    List<Query> findByWorkflow_Id(Long workflowId);
    
    List<Query> findByAssignedTeamAndStatus(QueryTeam assignedTeam, QueryStatus status);
    
    List<Query> findByStatusAndAssignedTeam(QueryStatus status, QueryTeam assignedTeam);
    
    Page<Query> findByAssignedTeamOrderByCreatedAtDesc(QueryTeam assignedTeam, Pageable pageable);
    
    // Query inbox functionality
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = :team AND q.status = :status ORDER BY q.createdAt DESC")
    List<Query> findByTeamAndStatus(@Param("team") QueryTeam team, @Param("status") QueryStatus status);
    
    // SLA tracking
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime")
    List<Query> findOverdueQueries(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Dashboard queries
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE q.assignedTeam = :team AND q.status = 'OPEN'")
    Long countOpenQueriesByTeam(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q.assignedTeam, COUNT(q) FROM Query q WHERE q.status = 'OPEN' GROUP BY q.assignedTeam")
    List<Object[]> getOpenQueryCountsByTeam();
    
    // Query resolution metrics
    @org.springframework.data.jpa.repository.Query("SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team")
    Double getAverageResolutionTimeByTeam(@Param("team") QueryTeam team);
    
    // Search and filtering
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN q.workflow w WHERE " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:team IS NULL OR q.assignedTeam = :team) AND " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) " +
           "ORDER BY q.createdAt DESC")
    List<Query> findQueriesWithFilters(
        @Param("status") QueryStatus status,
        @Param("team") QueryTeam team,
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode);
    
    // Team workload analysis
    @org.springframework.data.jpa.repository.Query(value = "SELECT assigned_team, COUNT(*) as openCount, AVG(EXTRACT(DAY FROM (CURRENT_TIMESTAMP - created_at)) * 24 + EXTRACT(HOUR FROM (CURRENT_TIMESTAMP - created_at))) as avgAgeHours FROM qrmfg_queries WHERE query_status = 'OPEN' GROUP BY assigned_team", nativeQuery = true)
    List<Object[]> getTeamWorkloadMetrics();
    
    // Additional methods needed by services
    Long countDistinctWorkflow_IdByStatus(QueryStatus status);
    Long countByStatus(QueryStatus status);
    Long countByStatusAndCreatedAtBefore(QueryStatus status, LocalDateTime dateTime);
    List<Query> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Query> findByCreatedAtAfter(LocalDateTime dateTime);
    List<Query> findByCreatedAtBefore(LocalDateTime dateTime);
    List<Query> findByStatusAndResolvedAtBetween(QueryStatus status, LocalDateTime start, LocalDateTime end);
    List<Query> findByStatusAndResolvedAtAfter(QueryStatus status, LocalDateTime dateTime);
    List<Query> findByStatusAndResolvedAtBefore(QueryStatus status, LocalDateTime dateTime);
    List<Query> findByStatus(QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT q.assignedTeam, COUNT(q) FROM Query q WHERE q.status = 'OPEN' GROUP BY q.assignedTeam")
    List<Object[]> countOpenQueriesByTeamGrouped();
    
    List<Query> findByRaisedBy(String raisedBy);
    List<Query> findByResolvedBy(String resolvedBy);
    List<Query> findByWorkflow_MaterialCode(String materialCode);
    List<Query> findByAssignedTeam(QueryTeam assignedTeam);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = :team AND q.status IN ('OPEN', 'IN_PROGRESS') ORDER BY q.createdAt DESC")
    List<Query> findTeamInboxQueries(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = :team AND q.status = 'RESOLVED' ORDER BY q.resolvedAt DESC")
    List<Query> findTeamResolvedQueries(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.raisedBy = :username ORDER BY q.createdAt DESC")
    List<Query> findQueriesRaisedByUser(@Param("username") String username);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.resolvedBy = :username ORDER BY q.resolvedAt DESC")
    List<Query> findQueriesResolvedByUser(@Param("username") String username);
    
    List<Query> findByWorkflow_IdAndStatus(Long workflowId, QueryStatus status);
    List<Query> findByPriorityLevel(String priorityLevel);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.createdAt >= :dateTime ORDER BY q.createdAt DESC")
    List<Query> findRecentQueries(@Param("dateTime") LocalDateTime dateTime);
    
    List<Query> findByResolvedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime")
    List<Query> findQueriesOverSLA(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team")
    Double getAverageResolutionTimeHours(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team AND q.resolvedAt BETWEEN :start AND :end")
    Double getAverageResolutionTimeHours(@Param("team") QueryTeam team, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    Long countByAssignedTeamAndStatus(QueryTeam team, QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime")
    Long countOverdueQueries(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE TRUNC(q.createdAt) = TRUNC(CURRENT_DATE)")
    Long countQueriesCreatedToday();
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE TRUNC(q.resolvedAt) = TRUNC(CURRENT_DATE) AND q.status = 'RESOLVED'")
    Long countQueriesResolvedToday();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.status IN ('OPEN', 'IN_PROGRESS') ORDER BY q.createdAt ASC")
    List<Query> findPendingQueriesForDashboard();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.priorityLevel = 'HIGH' AND q.status = 'OPEN' ORDER BY q.createdAt ASC")
    List<Query> findHighPriorityQueries();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime ORDER BY q.createdAt ASC")
    List<Query> findQueriesNeedingAttention(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.workflow.id = :workflowId AND q.status IN ('OPEN', 'IN_PROGRESS')")
    List<Query> findOpenQueriesByWorkflow(@Param("workflowId") Long workflowId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) > 0 FROM Query q WHERE q.workflow.id = :workflowId AND q.status IN ('OPEN', 'IN_PROGRESS')")
    Boolean hasWorkflowOpenQueries(@Param("workflowId") Long workflowId);
}