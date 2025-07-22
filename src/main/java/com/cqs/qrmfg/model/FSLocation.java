package com.cqs.qrmfg.model;

import javax.persistence.*;

@Entity
@Table(name = "FSLOCATION")
public class FSLocation {
    @Id
    @Column(name = "LOCATION_ID")
    private String locationId;

    @Column(name = "LOCATION_CODE", length = 50)
    private String locationCode;

    @Column(name = "LOCATION_NAME", length = 200)
    private String locationName;

    @Column(name = "LOCATION_TYPE", length = 50)
    private String locationType;

    @Column(name = "PARENT_LOCATION", length = 50)
    private String parentLocation;

    @Column(name = "STATUS", length = 20)
    private String status;

    public FSLocation() {}

    // Business logic methods
    public boolean isPlant() {
        return locationCode != null && locationCode.matches("^1\\d*$"); // Starts with 1
    }

    public boolean isBlock() {
        return locationCode != null && locationCode.contains("-"); // Contains dash for blocks
    }

    // Getters and setters
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getParentLocation() { return parentLocation; }
    public void setParentLocation(String parentLocation) { this.parentLocation = parentLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("FSLocation{locationCode='%s', locationName='%s', locationType='%s'}", 
                           locationCode, locationName, locationType);
    }
}