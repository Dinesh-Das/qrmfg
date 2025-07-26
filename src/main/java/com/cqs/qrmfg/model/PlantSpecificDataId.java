package com.cqs.qrmfg.model;

import java.io.Serializable;
import java.util.Objects;

public class PlantSpecificDataId implements Serializable {
    private String plantCode;
    private String materialCode;
    private String blockCode;
    
    public PlantSpecificDataId() {}
    
    public PlantSpecificDataId(String plantCode, String materialCode, String blockCode) {
        this.plantCode = plantCode;
        this.materialCode = materialCode;
        this.blockCode = blockCode;
    }
    
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    
    public String getBlockCode() { return blockCode; }
    public void setBlockCode(String blockCode) { this.blockCode = blockCode; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlantSpecificDataId that = (PlantSpecificDataId) o;
        return Objects.equals(plantCode, that.plantCode) &&
               Objects.equals(materialCode, that.materialCode) &&
               Objects.equals(blockCode, that.blockCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(plantCode, materialCode, blockCode);
    }
    
    @Override
    public String toString() {
        return String.format("PlantSpecificDataId{plant='%s', material='%s', block='%s'}", 
                           plantCode, materialCode, blockCode);
    }
}