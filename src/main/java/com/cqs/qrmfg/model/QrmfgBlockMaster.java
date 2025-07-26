package com.cqs.qrmfg.model;

import javax.persistence.*;

@Entity
@Table(name = "QRMFG_BLOCKS")
public class QrmfgBlockMaster {
    
    @Id
    @Column(name = "BLOCK_ID", length = 50)
    private String blockId;
    
    @Column(name = "DESCRIPTION", length = 200)
    private String description;
    
    public QrmfgBlockMaster() {}
    
    public QrmfgBlockMaster(String blockId, String description) {
        this.blockId = blockId;
        this.description = description;
    }
    
    // Getters and setters
    public String getBlockId() {
        return blockId;
    }
    
    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return String.format("QrmfgBlockMaster{blockId='%s', description='%s'}", 
                           blockId, description);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QrmfgBlockMaster that = (QrmfgBlockMaster) o;
        return blockId != null ? blockId.equals(that.blockId) : that.blockId == null;
    }
    
    @Override
    public int hashCode() {
        return blockId != null ? blockId.hashCode() : 0;
    }
}