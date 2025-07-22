package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.FSLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FSLocationRepository extends JpaRepository<FSLocation, String> {

    /**
     * Get all plant codes (location_code LIKE '1%')
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE '1%'")
    List<FSLocation> findPlantCodes();

    /**
     * Get blocks for a specific plant (location_code LIKE plantCode + '%')
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode")
    List<FSLocation> findBlocksByPlant(@Param("plantCode") String plantCode);

    /**
     * Find location by code
     */
    FSLocation findByLocationCode(String locationCode);

    /**
     * Get all distinct plant codes for dropdown
     */
    @Query("SELECT DISTINCT f.locationCode FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 ORDER BY f.locationCode")
    List<String> findDistinctPlantCodes();

    /**
     * Get all distinct block codes for a plant
     */
    @Query("SELECT DISTINCT f.locationCode FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode ORDER BY f.locationCode")
    List<String> findDistinctBlockCodesByPlant(@Param("plantCode") String plantCode);

    /**
     * Search plants by partial code
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 AND UPPER(f.locationCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) ORDER BY f.locationCode")
    List<FSLocation> searchPlantsByCode(@Param("searchTerm") String searchTerm);

    /**
     * Search blocks by plant and partial code
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode AND UPPER(f.locationCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) ORDER BY f.locationCode")
    List<FSLocation> searchBlocksByPlantAndTerm(@Param("plantCode") String plantCode, @Param("searchTerm") String searchTerm);

    /**
     * Count blocks by plant
     */
    @Query("SELECT COUNT(f) FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode")
    Long countBlocksByPlant(@Param("plantCode") String plantCode);

    /**
     * Find plants with block count
     */
    @Query("SELECT SUBSTRING(f.locationCode, 1, 4), COUNT(f) FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) > 4 GROUP BY SUBSTRING(f.locationCode, 1, 4) ORDER BY SUBSTRING(f.locationCode, 1, 4)")
    List<Object[]> findPlantsWithBlockCount();

    /**
     * Check if plant exists
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FSLocation f WHERE f.locationCode = :plantCode AND f.locationCode LIKE '1%'")
    Boolean existsPlantByCode(@Param("plantCode") String plantCode);

    /**
     * Check if block exists in plant
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FSLocation f WHERE f.locationCode = :blockCode AND f.locationCode LIKE CONCAT(:plantCode, '%')")
    Boolean existsBlockInPlant(@Param("plantCode") String plantCode, @Param("blockCode") String blockCode);

    /**
     * Get all locations (plants and blocks) for a specific pattern
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE :pattern ORDER BY f.locationCode")
    List<FSLocation> findByLocationCodePattern(@Param("pattern") String pattern);

    /**
     * Enhanced plant/block dropdown data queries
     */
    @Query("SELECT f.locationCode as plantCode, f.locationName as plantName, " +
           "(SELECT COUNT(f2) FROM FSLocation f2 WHERE f2.locationCode LIKE CONCAT(f.locationCode, '%') AND f2.locationCode != f.locationCode) as blockCount " +
           "FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 " +
           "ORDER BY f.locationCode")
    List<Object[]> findPlantsWithBlockCountDetailed();

    @Query("SELECT f.locationCode as blockCode, f.locationName as blockName, SUBSTRING(f.locationCode, 1, 4) as plantCode " +
           "FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode " +
           "ORDER BY f.locationCode")
    List<Object[]> findBlocksWithPlantInfo(@Param("plantCode") String plantCode);

    /**
     * Advanced search and filtering
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 AND " +
           "(:searchTerm IS NULL OR UPPER(f.locationCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR UPPER(f.locationName) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY f.locationCode")
    List<FSLocation> searchPlantsWithFilter(@Param("searchTerm") String searchTerm);

    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode AND " +
           "(:searchTerm IS NULL OR UPPER(f.locationCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR UPPER(f.locationName) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY f.locationCode")
    List<FSLocation> searchBlocksInPlantWithFilter(@Param("plantCode") String plantCode, @Param("searchTerm") String searchTerm);

    /**
     * Validation and data integrity queries
     */
    @Query("SELECT f.locationCode FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 AND f.locationCode NOT IN (SELECT DISTINCT SUBSTRING(f2.locationCode, 1, 4) FROM FSLocation f2 WHERE f2.locationCode LIKE '1%' AND LENGTH(f2.locationCode) > 4)")
    List<String> findPlantsWithoutBlocks();

    @Query("SELECT SUBSTRING(f.locationCode, 1, 4) as plantCode, COUNT(f) as blockCount FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) > 4 GROUP BY SUBSTRING(f.locationCode, 1, 4) ORDER BY blockCount DESC")
    List<Object[]> getBlockCountByPlant();

    /**
     * Status and active location queries
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE '1%' AND (:status IS NULL OR f.status = :status) ORDER BY f.locationCode")
    List<FSLocation> findLocationsByStatus(@Param("status") String status);

    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 AND f.status = 'ACTIVE'")
    List<FSLocation> findActivePlants();

    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode AND f.status = 'ACTIVE'")
    List<FSLocation> findActiveBlocksByPlant(@Param("plantCode") String plantCode);

    /**
     * Hierarchical location queries
     */
    @Query("SELECT f FROM FSLocation f WHERE f.parentLocation = :parentLocationCode ORDER BY f.locationCode")
    List<FSLocation> findByParentLocation(@Param("parentLocationCode") String parentLocationCode);

    @Query("SELECT f.locationCode, f.locationName, f.parentLocation FROM FSLocation f WHERE f.locationCode LIKE '1%' AND f.parentLocation IS NOT NULL ORDER BY f.parentLocation, f.locationCode")
    List<Object[]> findLocationHierarchy();

    /**
     * Bulk operations support
     */
    @Query("SELECT f FROM FSLocation f WHERE f.locationCode IN :locationCodes ORDER BY f.locationCode")
    List<FSLocation> findByLocationCodes(@Param("locationCodes") List<String> locationCodes);

    @Query("SELECT f FROM FSLocation f WHERE f.locationCode LIKE '1%' AND SUBSTRING(f.locationCode, 1, 4) IN :plantCodes ORDER BY f.locationCode")
    List<FSLocation> findLocationsByPlantCodes(@Param("plantCodes") List<String> plantCodes);

    /**
     * Performance optimization queries
     */
    @Query("SELECT f.locationCode, f.locationName FROM FSLocation f WHERE f.locationCode LIKE '1%' AND LENGTH(f.locationCode) = 4 ORDER BY f.locationCode")
    List<Object[]> findPlantCodesAndNames();

    @Query("SELECT f.locationCode, f.locationName FROM FSLocation f WHERE f.locationCode LIKE CONCAT(:plantCode, '%') AND f.locationCode != :plantCode ORDER BY f.locationCode")
    List<Object[]> findBlockCodesAndNamesByPlant(@Param("plantCode") String plantCode);
}