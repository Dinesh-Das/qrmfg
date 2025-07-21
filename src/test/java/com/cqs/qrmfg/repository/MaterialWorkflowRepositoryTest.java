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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public class MaterialWorkflowRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MaterialWorkflowRepository workflowRepository;

    private MaterialWorkflow testWorkflow1;
    private MaterialWorkflow testWorkflow2;
    private MaterialWorkflow testWorkflow3;

    @BeforeEach
    void setUp() {
        // Create test workflows
        testWorkflow1 = new MaterialWorkflow("CHEM-001", "jvc.user", "Plant A");
        testWorkflow1.setMaterialName("Chemical A");
        testWorkflow1.setState(WorkflowState.PLANT_PENDING);
        testWorkflow1.setExtendedAt(LocalDateTime.now().minusDays(2));

        testWorkflow2 = new MaterialWorkflow("CHEM-002", "jvc.user", "Plant B");
        testWorkflow2.setMaterialName("Chemical B");
        testWorkflow2.setState(WorkflowState.CQS_PENDING);
        testWorkflow2.setExtendedAt(LocalDateTime.now().minusDays(5)); // Overdue

        testWorkflow3 = new MaterialWorkflow("CHEM-003", "jvc.user2", "Plant A");
        testWorkflow3.setMaterialName("Chemical C");
        testWorkflow3.setState(WorkflowState.COMPLETED);
        testWorkflow3.setCompletedAt(LocalDateTime.now().minusDays(1));

        // Persist test data
        entityManager.persistAndFlush(testWorkflow1);
        entityManager.persistAndFlush(testWorkflow2);
        entityManager.persistAndFlush(testWorkflow3);

        // Add queries to test workflow with open queries
        Query openQuery = new Query(testWorkflow1, "Test question", QueryTeam.CQS, "plant.user");
        entityManager.persistAndFlush(openQuery);
    }

    @Test
    void testFindByMaterialId() {
        // When
        Optional<MaterialWorkflow> result = workflowRepository.findByMaterialId("CHEM-001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getMaterialName()).isEqualTo("Chemical A");
        assertThat(result.get().getAssignedPlant()).isEqualTo("Plant A");
    }

    @Test
    void testFindByState() {
        // When
        List<MaterialWorkflow> pendingWorkflows = workflowRepository.findByState(WorkflowState.PLANT_PENDING);
        List<MaterialWorkflow> completedWorkflows = workflowRepository.findByState(WorkflowState.COMPLETED);

        // Then
        assertThat(pendingWorkflows).hasSize(1);
        assertThat(pendingWorkflows.get(0).getMaterialId()).isEqualTo("CHEM-001");

        assertThat(completedWorkflows).hasSize(1);
        assertThat(completedWorkflows.get(0).getMaterialId()).isEqualTo("CHEM-003");
    }

    @Test
    void testFindByAssignedPlant() {
        // When
        List<MaterialWorkflow> plantAWorkflows = workflowRepository.findByAssignedPlant("Plant A");
        List<MaterialWorkflow> plantBWorkflows = workflowRepository.findByAssignedPlant("Plant B");

        // Then
        assertThat(plantAWorkflows).hasSize(2);
        assertThat(plantBWorkflows).hasSize(1);
        assertThat(plantBWorkflows.get(0).getMaterialId()).isEqualTo("CHEM-002");
    }

    @Test
    void testFindByInitiatedBy() {
        // When
        List<MaterialWorkflow> jvcUserWorkflows = workflowRepository.findByInitiatedBy("jvc.user");
        List<MaterialWorkflow> jvcUser2Workflows = workflowRepository.findByInitiatedBy("jvc.user2");

        // Then
        assertThat(jvcUserWorkflows).hasSize(2);
        assertThat(jvcUser2Workflows).hasSize(1);
        assertThat(jvcUser2Workflows.get(0).getMaterialId()).isEqualTo("CHEM-003");
    }

    @Test
    void testFindPendingWorkflows() {
        // When
        List<MaterialWorkflow> pendingWorkflows = workflowRepository.findPendingWorkflows();

        // Then
        assertThat(pendingWorkflows).hasSize(2); // CHEM-001 and CHEM-002 are not completed
        assertThat(pendingWorkflows).extracting(MaterialWorkflow::getMaterialId)
                .containsExactlyInAnyOrder("CHEM-001", "CHEM-002");
    }

    @Test
    void testFindWorkflowsWithOpenQueries() {
        // When
        List<MaterialWorkflow> workflowsWithQueries = workflowRepository.findWorkflowsWithOpenQueries();

        // Then
        assertThat(workflowsWithQueries).hasSize(1);
        assertThat(workflowsWithQueries.get(0).getMaterialId()).isEqualTo("CHEM-001");
    }

    @Test
    void testCountByState() {
        // When
        long plantPendingCount = workflowRepository.countByState(WorkflowState.PLANT_PENDING);
        long cqsPendingCount = workflowRepository.countByState(WorkflowState.CQS_PENDING);
        long completedCount = workflowRepository.countByState(WorkflowState.COMPLETED);

        // Then
        assertThat(plantPendingCount).isEqualTo(1);
        assertThat(cqsPendingCount).isEqualTo(1);
        assertThat(completedCount).isEqualTo(1);
    }

    @Test
    void testCountWorkflowsWithOpenQueries() {
        // When
        long count = workflowRepository.countWorkflowsWithOpenQueries();

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindByCreatedAtAfter() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);

        // When
        List<MaterialWorkflow> recentWorkflows = workflowRepository.findByCreatedAtAfter(cutoffDate);

        // Then
        assertThat(recentWorkflows).hasSize(3); // All workflows were created today
    }

    @Test
    void testFindByCompletedAtAfter() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(2);

        // When
        List<MaterialWorkflow> recentlyCompleted = workflowRepository.findByCompletedAtAfter(cutoffDate);

        // Then
        assertThat(recentlyCompleted).hasSize(1);
        assertThat(recentlyCompleted.get(0).getMaterialId()).isEqualTo("CHEM-003");
    }

    @Test
    void testExistsByMaterialId() {
        // When & Then
        assertThat(workflowRepository.existsByMaterialId("CHEM-001")).isTrue();
        assertThat(workflowRepository.existsByMaterialId("CHEM-999")).isFalse();
    }

    @Test
    void testFindByPlantAndState() {
        // When
        List<MaterialWorkflow> plantAPending = workflowRepository.findByPlantAndState("Plant A", WorkflowState.PLANT_PENDING);
        List<MaterialWorkflow> plantACompleted = workflowRepository.findByPlantAndState("Plant A", WorkflowState.COMPLETED);

        // Then
        assertThat(plantAPending).hasSize(1);
        assertThat(plantAPending.get(0).getMaterialId()).isEqualTo("CHEM-001");

        assertThat(plantACompleted).hasSize(1);
        assertThat(plantACompleted.get(0).getMaterialId()).isEqualTo("CHEM-003");
    }

    @Test
    void testFindPendingByPlant() {
        // When
        List<MaterialWorkflow> plantAPending = workflowRepository.findPendingByPlant("Plant A");
        List<MaterialWorkflow> plantBPending = workflowRepository.findPendingByPlant("Plant B");

        // Then
        assertThat(plantAPending).hasSize(1); // CHEM-001 is pending, CHEM-003 is completed
        assertThat(plantAPending.get(0).getMaterialId()).isEqualTo("CHEM-001");

        assertThat(plantBPending).hasSize(1);
        assertThat(plantBPending.get(0).getMaterialId()).isEqualTo("CHEM-002");
    }

    @Test
    void testFindPendingByInitiatedBy() {
        // When
        List<MaterialWorkflow> jvcUserPending = workflowRepository.findPendingByInitiatedBy("jvc.user");
        List<MaterialWorkflow> jvcUser2Pending = workflowRepository.findPendingByInitiatedBy("jvc.user2");

        // Then
        assertThat(jvcUserPending).hasSize(2); // CHEM-001 and CHEM-002 are pending
        assertThat(jvcUser2Pending).hasSize(0); // CHEM-003 is completed
    }

    @Test
    void testSaveAndUpdate() {
        // Given
        MaterialWorkflow newWorkflow = new MaterialWorkflow("CHEM-004", "jvc.user", "Plant C");
        newWorkflow.setMaterialName("Chemical D");

        // When - Save
        MaterialWorkflow saved = workflowRepository.save(newWorkflow);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMaterialId()).isEqualTo("CHEM-004");

        // When - Update
        saved.setState(WorkflowState.PLANT_PENDING);
        saved.setExtendedAt(LocalDateTime.now());
        MaterialWorkflow updated = workflowRepository.save(saved);

        // Then
        assertThat(updated.getState()).isEqualTo(WorkflowState.PLANT_PENDING);
        assertThat(updated.getExtendedAt()).isNotNull();
    }

    @Test
    void testDelete() {
        // Given
        Long workflowId = testWorkflow1.getId();

        // When
        workflowRepository.deleteById(workflowId);

        // Then
        Optional<MaterialWorkflow> deleted = workflowRepository.findById(workflowId);
        assertThat(deleted).isEmpty();
    }
}