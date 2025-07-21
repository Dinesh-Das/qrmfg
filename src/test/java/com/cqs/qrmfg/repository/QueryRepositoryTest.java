package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public class QueryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QueryRepository queryRepository;

    private MaterialWorkflow testWorkflow1;
    private MaterialWorkflow testWorkflow2;
    private Query testQuery1;
    private Query testQuery2;
    private Query testQuery3;

    @BeforeEach
    void setUp() {
        // Create test workflows
        testWorkflow1 = new MaterialWorkflow("CHEM-001", "jvc.user", "Plant A");
        testWorkflow1.setMaterialName("Chemical A");
        testWorkflow1.setState(WorkflowState.PLANT_PENDING);

        testWorkflow2 = new MaterialWorkflow("CHEM-002", "jvc.user", "Plant B");
        testWorkflow2.setMaterialName("Chemical B");
        testWorkflow2.setState(WorkflowState.CQS_PENDING);

        entityManager.persistAndFlush(testWorkflow1);
        entityManager.persistAndFlush(testWorkflow2);

        // Create test queries
        testQuery1 = new Query(testWorkflow1, "What is the flash point?", 5, "flashPoint", QueryTeam.CQS, "plant.user");
        testQuery1.setPriorityLevel("HIGH");
        testQuery1.setQueryCategory("SAFETY");

        testQuery2 = new Query(testWorkflow1, "What is the boiling point?", 6, "boilingPoint", QueryTeam.TECH, "plant.user");
        testQuery2.setPriorityLevel("NORMAL");

        testQuery3 = new Query(testWorkflow2, "Material composition?", QueryTeam.CQS, "plant.user2");
        testQuery3.resolve("Contains 95% active ingredient", "cqs.user");
        testQuery3.setPriorityLevel("LOW");

        entityManager.persistAndFlush(testQuery1);
        entityManager.persistAndFlush(testQuery2);
        entityManager.persistAndFlush(testQuery3);
    }

    @Test
    void testFindByWorkflowId() {
        // When
        List<Query> workflow1Queries = queryRepository.findByWorkflowId(testWorkflow1.getId());
        List<Query> workflow2Queries = queryRepository.findByWorkflowId(testWorkflow2.getId());

        // Then
        assertThat(workflow1Queries).hasSize(2);
        assertThat(workflow2Queries).hasSize(1);
        assertThat(workflow2Queries.get(0).getQuestion()).isEqualTo("Material composition?");
    }

    @Test
    void testFindByMaterialId() {
        // When
        List<Query> chem001Queries = queryRepository.findByMaterialId("CHEM-001");
        List<Query> chem002Queries = queryRepository.findByMaterialId("CHEM-002");

        // Then
        assertThat(chem001Queries).hasSize(2);
        assertThat(chem002Queries).hasSize(1);
    }

    @Test
    void testFindByStatus() {
        // When
        List<Query> openQueries = queryRepository.findByStatus(QueryStatus.OPEN);
        List<Query> resolvedQueries = queryRepository.findByStatus(QueryStatus.RESOLVED);

        // Then
        assertThat(openQueries).hasSize(2);
        assertThat(resolvedQueries).hasSize(1);
        assertThat(resolvedQueries.get(0).getResponse()).isEqualTo("Contains 95% active ingredient");
    }

    @Test
    void testFindByAssignedTeam() {
        // When
        List<Query> cqsQueries = queryRepository.findByAssignedTeam(QueryTeam.CQS);
        List<Query> techQueries = queryRepository.findByAssignedTeam(QueryTeam.TECH);

        // Then
        assertThat(cqsQueries).hasSize(2);
        assertThat(techQueries).hasSize(1);
        assertThat(techQueries.get(0).getFieldName()).isEqualTo("boilingPoint");
    }

    @Test
    void testFindByRaisedBy() {
        // When
        List<Query> plantUserQueries = queryRepository.findByRaisedBy("plant.user");
        List<Query> plantUser2Queries = queryRepository.findByRaisedBy("plant.user2");

        // Then
        assertThat(plantUserQueries).hasSize(2);
        assertThat(plantUser2Queries).hasSize(1);
    }

    @Test
    void testFindByResolvedBy() {
        // When
        List<Query> cqsUserResolved = queryRepository.findByResolvedBy("cqs.user");

        // Then
        assertThat(cqsUserResolved).hasSize(1);
        assertThat(cqsUserResolved.get(0).getQuestion()).isEqualTo("Material composition?");
    }

    @Test
    void testFindByWorkflowIdAndStatus() {
        // When
        List<Query> workflow1Open = queryRepository.findByWorkflowIdAndStatus(testWorkflow1.getId(), QueryStatus.OPEN);
        List<Query> workflow2Resolved = queryRepository.findByWorkflowIdAndStatus(testWorkflow2.getId(), QueryStatus.RESOLVED);

        // Then
        assertThat(workflow1Open).hasSize(2);
        assertThat(workflow2Resolved).hasSize(1);
    }

    @Test
    void testFindByAssignedTeamAndStatus() {
        // When
        List<Query> cqsOpen = queryRepository.findByAssignedTeamAndStatus(QueryTeam.CQS, QueryStatus.OPEN);
        List<Query> cqsResolved = queryRepository.findByAssignedTeamAndStatus(QueryTeam.CQS, QueryStatus.RESOLVED);

        // Then
        assertThat(cqsOpen).hasSize(1);
        assertThat(cqsResolved).hasSize(1);
    }

    @Test
    void testFindByPriorityLevel() {
        // When
        List<Query> highPriorityQueries = queryRepository.findByPriorityLevel("HIGH");
        List<Query> normalPriorityQueries = queryRepository.findByPriorityLevel("NORMAL");

        // Then
        assertThat(highPriorityQueries).hasSize(1);
        assertThat(highPriorityQueries.get(0).getFieldName()).isEqualTo("flashPoint");
        assertThat(normalPriorityQueries).hasSize(1);
    }

    @Test
    void testFindByQueryCategory() {
        // When
        List<Query> safetyQueries = queryRepository.findByQueryCategory("SAFETY");

        // Then
        assertThat(safetyQueries).hasSize(1);
        assertThat(safetyQueries.get(0).getQuestion()).isEqualTo("What is the flash point?");
    }

    @Test
    void testFindPendingQueriesForDashboard() {
        // When
        List<Query> pendingQueries = queryRepository.findPendingQueriesForDashboard();

        // Then
        assertThat(pendingQueries).hasSize(2);
        // Should be ordered by creation time (oldest first)
    }

    @Test
    void testFindHighPriorityQueries() {
        // When
        List<Query> highPriorityQueries = queryRepository.findHighPriorityQueries();

        // Then
        assertThat(highPriorityQueries).hasSize(1);
        assertThat(highPriorityQueries.get(0).getPriorityLevel()).isEqualTo("HIGH");
    }

    @Test
    void testCountByStatus() {
        // When
        long openCount = queryRepository.countByStatus(QueryStatus.OPEN);
        long resolvedCount = queryRepository.countByStatus(QueryStatus.RESOLVED);

        // Then
        assertThat(openCount).isEqualTo(2);
        assertThat(resolvedCount).isEqualTo(1);
    }

    @Test
    void testCountByAssignedTeam() {
        // When
        long cqsCount = queryRepository.countByAssignedTeam(QueryTeam.CQS);
        long techCount = queryRepository.countByAssignedTeam(QueryTeam.TECH);

        // Then
        assertThat(cqsCount).isEqualTo(2);
        assertThat(techCount).isEqualTo(1);
    }

    @Test
    void testCountByAssignedTeamAndStatus() {
        // When
        long cqsOpenCount = queryRepository.countByAssignedTeamAndStatus(QueryTeam.CQS, QueryStatus.OPEN);
        long cqsResolvedCount = queryRepository.countByAssignedTeamAndStatus(QueryTeam.CQS, QueryStatus.RESOLVED);

        // Then
        assertThat(cqsOpenCount).isEqualTo(1);
        assertThat(cqsResolvedCount).isEqualTo(1);
    }

    @Test
    void testFindOpenQueriesByWorkflow() {
        // When
        List<Query> openQueries = queryRepository.findOpenQueriesByWorkflow(testWorkflow1.getId());

        // Then
        assertThat(openQueries).hasSize(2);
    }

    @Test
    void testHasWorkflowOpenQueries() {
        // When
        boolean workflow1HasOpen = queryRepository.hasWorkflowOpenQueries(testWorkflow1.getId());
        boolean workflow2HasOpen = queryRepository.hasWorkflowOpenQueries(testWorkflow2.getId());

        // Then
        assertThat(workflow1HasOpen).isTrue();
        assertThat(workflow2HasOpen).isFalse(); // Only resolved query
    }

    @Test
    void testFindTeamInboxQueries() {
        // When
        List<Query> cqsInbox = queryRepository.findTeamInboxQueries(QueryTeam.CQS);
        List<Query> techInbox = queryRepository.findTeamInboxQueries(QueryTeam.TECH);

        // Then
        assertThat(cqsInbox).hasSize(1); // Only open CQS queries
        assertThat(techInbox).hasSize(1);
    }

    @Test
    void testFindTeamResolvedQueries() {
        // When
        List<Query> cqsResolved = queryRepository.findTeamResolvedQueries(QueryTeam.CQS);
        List<Query> techResolved = queryRepository.findTeamResolvedQueries(QueryTeam.TECH);

        // Then
        assertThat(cqsResolved).hasSize(1);
        assertThat(techResolved).hasSize(0);
    }

    @Test
    void testFindQueriesRaisedByUser() {
        // When
        List<Query> plantUserQueries = queryRepository.findQueriesRaisedByUser("plant.user");

        // Then
        assertThat(plantUserQueries).hasSize(2);
        // Should be ordered by creation time (newest first)
    }

    @Test
    void testFindQueriesResolvedByUser() {
        // When
        List<Query> cqsUserResolved = queryRepository.findQueriesResolvedByUser("cqs.user");

        // Then
        assertThat(cqsUserResolved).hasSize(1);
        assertThat(cqsUserResolved.get(0).getResolvedBy()).isEqualTo("cqs.user");
    }

    @Test
    void testFindByStepNumber() {
        // When
        List<Query> step5Queries = queryRepository.findByStepNumber(5);
        List<Query> step6Queries = queryRepository.findByStepNumber(6);

        // Then
        assertThat(step5Queries).hasSize(1);
        assertThat(step5Queries.get(0).getFieldName()).isEqualTo("flashPoint");
        assertThat(step6Queries).hasSize(1);
    }

    @Test
    void testFindByFieldName() {
        // When
        List<Query> flashPointQueries = queryRepository.findByFieldName("flashPoint");
        List<Query> boilingPointQueries = queryRepository.findByFieldName("boilingPoint");

        // Then
        assertThat(flashPointQueries).hasSize(1);
        assertThat(boilingPointQueries).hasSize(1);
    }

    @Test
    void testFindByStepNumberAndFieldName() {
        // When
        List<Query> step5FlashPoint = queryRepository.findByStepNumberAndFieldName(5, "flashPoint");

        // Then
        assertThat(step5FlashPoint).hasSize(1);
        assertThat(step5FlashPoint.get(0).getQuestion()).isEqualTo("What is the flash point?");
    }

    @Test
    void testFindRecentQueries() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(1);

        // When
        List<Query> recentQueries = queryRepository.findRecentQueries(cutoffDate);

        // Then
        assertThat(recentQueries).hasSize(3); // All queries were created recently
    }

    @Test
    void testSaveAndUpdate() {
        // Given
        Query newQuery = new Query(testWorkflow1, "New test question", QueryTeam.CQS, "plant.user");

        // When - Save
        Query saved = queryRepository.save(newQuery);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getQuestion()).isEqualTo("New test question");

        // When - Update
        saved.resolve("Test response", "cqs.user");
        Query updated = queryRepository.save(saved);

        // Then
        assertThat(updated.getStatus()).isEqualTo(QueryStatus.RESOLVED);
        assertThat(updated.getResponse()).isEqualTo("Test response");
        assertThat(updated.getResolvedBy()).isEqualTo("cqs.user");
    }
}