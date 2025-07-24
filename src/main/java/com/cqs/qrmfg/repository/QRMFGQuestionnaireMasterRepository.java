package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QRMFGQuestionnaireMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QRMFGQuestionnaireMasterRepository extends JpaRepository<QRMFGQuestionnaireMaster, Long> {

    /**
     * Get all active questionnaire templates ordered by step and order index
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findActiveQuestionnaires();

    /**
     * Get questionnaire templates by step number
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.stepNumber = :stepNumber ORDER BY q.orderIndex")
    List<QRMFGQuestionnaireMaster> findByStepNumber(@Param("stepNumber") Integer stepNumber);

    /**
     * Get questionnaire templates by category
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.category = :category ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findByCategory(@Param("category") String category);

    /**
     * Get questionnaire template by question ID
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.questionId = :questionId")
    QRMFGQuestionnaireMaster findByQuestionId(@Param("questionId") String questionId);

    /**
     * Get all distinct step numbers for active questionnaires
     */
    @Query("SELECT DISTINCT q.stepNumber FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true ORDER BY q.stepNumber")
    List<Integer> findDistinctStepNumbers();

    /**
     * Get all distinct categories for active questionnaires
     */
    @Query("SELECT DISTINCT q.category FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.category IS NOT NULL ORDER BY q.category")
    List<String> findDistinctCategories();

    /**
     * Count total active questions
     */
    @Query("SELECT COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true")
    Long countActiveQuestions();

    /**
     * Count questions by step
     */
    @Query("SELECT COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.stepNumber = :stepNumber")
    Long countQuestionsByStep(@Param("stepNumber") Integer stepNumber);

    /**
     * Get questionnaire templates by field type
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.questionType = :questionType ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findByFieldType(@Param("questionType") String questionType);

    /**
     * Get required questionnaire templates
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.isRequired = true ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findRequiredQuestions();

    /**
     * Get questionnaire templates by step and category
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.stepNumber = :stepNumber AND q.category = :category ORDER BY q.orderIndex")
    List<QRMFGQuestionnaireMaster> findByStepAndCategory(@Param("stepNumber") Integer stepNumber, @Param("category") String category);

    /**
     * Search questionnaire templates by question text
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND UPPER(q.questionText) LIKE UPPER(CONCAT('%', :searchTerm, '%')) ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> searchByQuestionText(@Param("searchTerm") String searchTerm);

    /**
     * Get questionnaire templates with validation rules
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.validationRules IS NOT NULL ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findQuestionsWithValidation();

    /**
     * Get questionnaire templates by display order range
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.orderIndex BETWEEN :startOrder AND :endOrder ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findByOrderRange(@Param("startOrder") Integer startOrder, @Param("endOrder") Integer endOrder);

    /**
     * Count questions by category
     */
    @Query("SELECT COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.category = :category")
    Long countQuestionsByCategory(@Param("category") String category);

    /**
     * Count required questions by step
     */
    @Query("SELECT COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.stepNumber = :stepNumber AND q.isRequired = true")
    Long countRequiredQuestionsByStep(@Param("stepNumber") Integer stepNumber);

    /**
     * Get questions grouped by step with counts
     */
    @Query("SELECT q.stepNumber, COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true GROUP BY q.stepNumber ORDER BY q.stepNumber")
    List<Object[]> getQuestionCountByStep();

    /**
     * Get questions grouped by category with counts
     */
    @Query("SELECT q.category, COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.category IS NOT NULL GROUP BY q.category ORDER BY q.category")
    List<Object[]> getQuestionCountByCategory();

    /**
     * Get questions grouped by field type with counts
     */
    @Query("SELECT q.questionType, COUNT(q) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true GROUP BY q.questionType ORDER BY q.questionType")
    List<Object[]> getQuestionCountByFieldType();

    /**
     * Find questions with dependencies
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.dependsOnQuestionId IS NOT NULL ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findQuestionsWithDependencies();

    /**
     * Find questions that depend on a specific question
     */
    @Query("SELECT q FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.dependsOnQuestionId = :questionId ORDER BY q.stepNumber, q.orderIndex")
    List<QRMFGQuestionnaireMaster> findDependentQuestions(@Param("questionId") String questionId);

    /**
     * Get maximum step number
     */
    @Query("SELECT MAX(q.stepNumber) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true")
    Integer getMaxStepNumber();

    /**
     * Get maximum order index for a step
     */
    @Query("SELECT MAX(q.orderIndex) FROM QRMFGQuestionnaireMaster q WHERE q.isActive = true AND q.stepNumber = :stepNumber")
    Integer getMaxOrderIndexForStep(@Param("stepNumber") Integer stepNumber);
}