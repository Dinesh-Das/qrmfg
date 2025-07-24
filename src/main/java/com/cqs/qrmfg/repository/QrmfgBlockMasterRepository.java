package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QrmfgBlockMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrmfgBlockMasterRepository extends JpaRepository<QrmfgBlockMaster, String> {
    
    /**
     * Find block by block ID
     */
    Optional<QrmfgBlockMaster> findByBlockId(String blockId);
    
    /**
     * Find blocks by description containing text (case insensitive)
     */
    @Query("SELECT b FROM QrmfgBlockMaster b WHERE UPPER(b.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))")
    List<QrmfgBlockMaster> findByDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    /**
     * Find all blocks ordered by block ID
     */
    @Query("SELECT b FROM QrmfgBlockMaster b ORDER BY b.blockId")
    List<QrmfgBlockMaster> findAllOrderByBlockId();
    
    /**
     * Search blocks by block ID or description
     */
    @Query("SELECT b FROM QrmfgBlockMaster b WHERE " +
           "UPPER(b.blockId) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(b.description) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY b.blockId")
    List<QrmfgBlockMaster> searchBlocks(@Param("searchTerm") String searchTerm);
    
    /**
     * Check if block ID exists
     */
    boolean existsByBlockId(String blockId);
    
    /**
     * Count total blocks
     */
    @Query("SELECT COUNT(b) FROM QrmfgBlockMaster b")
    Long countAllBlocks();
}