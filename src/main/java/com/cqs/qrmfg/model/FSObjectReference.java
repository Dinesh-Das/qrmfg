package com.cqs.qrmfg.model;

import javax.persistence.*;

@Entity
@Table(name = "FSOBJECTREFERENCE")
public class FSObjectReference {
    @Id
    @Column(name = "OBJECT_ID")
    private String objectId;

    @Column(name = "OBJECT_TYPE", length = 50)
    private String objectType;

    @Column(name = "OBJECT_KEY", length = 100)
    private String objectKey;

    @Column(name = "R_OBJECT_TYPE", length = 50)
    private String rObjectType;

    @Column(name = "R_OBJECT_KEY", length = 100)
    private String rObjectKey;

    @Column(name = "R_OBJECT_DESC", length = 500)
    private String rObjectDesc;

    @Column(name = "REF_CODE", length = 50)
    private String refCode;

    public FSObjectReference() {}

    // Getters and setters
    public String getObjectId() { return objectId; }
    public void setObjectId(String objectId) { this.objectId = objectId; }

    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getRObjectType() { return rObjectType; }
    public void setRObjectType(String rObjectType) { this.rObjectType = rObjectType; }

    public String getRObjectKey() { return rObjectKey; }
    public void setRObjectKey(String rObjectKey) { this.rObjectKey = rObjectKey; }

    public String getRObjectDesc() { return rObjectDesc; }
    public void setRObjectDesc(String rObjectDesc) { this.rObjectDesc = rObjectDesc; }

    public String getRefCode() { return refCode; }
    public void setRefCode(String refCode) { this.refCode = refCode; }

    @Override
    public String toString() {
        return String.format("FSObjectReference{objectType='%s', objectKey='%s', rObjectType='%s', rObjectKey='%s'}", 
                           objectType, objectKey, rObjectType, rObjectKey);
    }
}