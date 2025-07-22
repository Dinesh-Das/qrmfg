package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.FSObjectReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FSObjectReferenceRepository extends JpaRepository<FSObjectReference, Long> {

    /**
     * Get all active projects (object_type='PROJECT' and object_key LIKE 'SER%')
     */
    @Query("SELECT DISTINCT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%'")
    List<FSObjectReference> findActiveProjects();

    /**
     * Get materials for a specific project
     */
    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM'")
    List<FSObjectReference> findMaterialsByProject(@Param("projectCode") String projectCode);

    /**
     * Find project by object key
     */
    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :objectKey")
    FSObjectReference findProjectByObjectKey(@Param("objectKey") String objectKey);

    /**
     * Find material by project and material code
     */
    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectKey = :materialCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM'")
    FSObjectReference findMaterialByProjectAndCode(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Get all distinct project codes for dropdown
     */
    @Query("SELECT DISTINCT f.objectKey FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' ORDER BY f.objectKey")
    List<String> findDistinctProjectCodes();

    /**
     * Get all distinct material codes for a project
     */
    @Query("SELECT DISTINCT f.rObjectKey FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' ORDER BY f.rObjectKey")
    List<String> findDistinctMaterialCodesByProject(@Param("projectCode") String projectCode);

    /**
     * Search projects by partial code
     */
    @Query("SELECT DISTINCT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND UPPER(f.objectKey) LIKE UPPER(CONCAT('%', :searchTerm, '%')) ORDER BY f.objectKey")
    List<FSObjectReference> searchProjectsByCode(@Param("searchTerm") String searchTerm);

    /**
     * Search materials by partial code or description
     */
    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' AND (UPPER(f.rObjectKey) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR UPPER(f.rObjectDesc) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) ORDER BY f.rObjectKey")
    List<FSObjectReference> searchMaterialsByProjectAndTerm(@Param("projectCode") String projectCode, @Param("searchTerm") String searchTerm);

    /**
     * Count materials by project
     */
    @Query("SELECT COUNT(f) FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM'")
    Long countMaterialsByProject(@Param("projectCode") String projectCode);

    /**
     * Find projects with materials count
     */
    @Query("SELECT f.objectKey, COUNT(f) FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' GROUP BY f.objectKey ORDER BY f.objectKey")
    List<Object[]> findProjectsWithMaterialCount();

    /**
     * Check if project exists
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode")
    Boolean existsProjectByCode(@Param("projectCode") String projectCode);

    /**
     * Check if material exists in project
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectKey = :materialCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM'")
    Boolean existsMaterialInProject(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Enhanced project/material dropdown data queries
     */
    @Query("SELECT DISTINCT f.objectKey as projectCode, f.objectKey as projectName FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND f.objectKey IN (SELECT DISTINCT f2.objectKey FROM FSObjectReference f2 WHERE f2.objectType = 'PROJECT' AND f2.rObjectType = 'ITEM' AND f2.refCode = 'SER_PRD_ITEM') ORDER BY f.objectKey")
    List<Object[]> findActiveProjectsWithMaterials();

    @Query("SELECT f.rObjectKey as materialCode, f.rObjectDesc as materialDescription, COUNT(DISTINCT f.objectKey) as projectCount FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' GROUP BY f.rObjectKey, f.rObjectDesc ORDER BY f.rObjectKey")
    List<Object[]> findMaterialsWithProjectCount();

    @Query("SELECT f.objectKey as projectCode, f.rObjectKey as materialCode, f.rObjectDesc as materialDescription FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' ORDER BY f.objectKey, f.rObjectKey")
    List<Object[]> findAllProjectMaterialMappings();

    /**
     * Advanced search and filtering
     */
    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND " +
           "(:searchTerm IS NULL OR UPPER(f.objectKey) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY f.objectKey")
    List<FSObjectReference> searchProjects(@Param("searchTerm") String searchTerm);

    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey = :projectCode AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' AND " +
           "(:searchTerm IS NULL OR UPPER(f.rObjectKey) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR UPPER(f.rObjectDesc) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY f.rObjectKey")
    List<FSObjectReference> searchMaterialsInProject(@Param("projectCode") String projectCode, @Param("searchTerm") String searchTerm);

    /**
     * Validation and data integrity queries
     */
    @Query("SELECT f.objectKey FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND f.objectKey NOT IN (SELECT DISTINCT f2.objectKey FROM FSObjectReference f2 WHERE f2.objectType = 'PROJECT' AND f2.rObjectType = 'ITEM' AND f2.refCode = 'SER_PRD_ITEM')")
    List<String> findProjectsWithoutMaterials();

    @Query("SELECT f.rObjectKey FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' GROUP BY f.rObjectKey HAVING COUNT(DISTINCT f.objectKey) = 1")
    List<String> findMaterialsInSingleProject();

    /**
     * Performance optimization queries
     */
    @Query("SELECT f.objectKey, COUNT(f.rObjectKey) FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey LIKE 'SER%' AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' GROUP BY f.objectKey HAVING COUNT(f.rObjectKey) > :minMaterialCount ORDER BY COUNT(f.rObjectKey) DESC")
    List<Object[]> findProjectsWithMaterialCountAbove(@Param("minMaterialCount") Long minMaterialCount);

    @Query("SELECT f.rObjectKey, f.rObjectDesc, COUNT(DISTINCT f.objectKey) as projectCount FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM' GROUP BY f.rObjectKey, f.rObjectDesc HAVING COUNT(DISTINCT f.objectKey) > :minProjectCount ORDER BY COUNT(DISTINCT f.objectKey) DESC")
    List<Object[]> findMaterialsInMultipleProjects(@Param("minProjectCount") Long minProjectCount);

    /**
     * Bulk operations support
     */
    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.objectKey IN :projectCodes AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM'")
    List<FSObjectReference> findMaterialsByProjectCodes(@Param("projectCodes") List<String> projectCodes);

    @Query("SELECT f FROM FSObjectReference f WHERE f.objectType = 'PROJECT' AND f.rObjectKey IN :materialCodes AND f.rObjectType = 'ITEM' AND f.refCode = 'SER_PRD_ITEM'")
    List<FSObjectReference> findProjectsByMaterialCodes(@Param("materialCodes") List<String> materialCodes);
}