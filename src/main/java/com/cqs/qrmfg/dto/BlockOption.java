package com.cqs.qrmfg.dto;

/**
 * DTO for block dropdown options
 */
public class BlockOption {
    private String value; // location_code (e.g., "1001-A")
    private String label; // location_code (same as value for display)
    private String plantCode; // parent plant code

    public BlockOption() {}

    public BlockOption(String value, String label, String plantCode) {
        this.value = value;
        this.label = label;
        this.plantCode = plantCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }
}