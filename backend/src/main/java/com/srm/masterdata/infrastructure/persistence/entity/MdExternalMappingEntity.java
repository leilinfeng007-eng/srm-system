package com.srm.masterdata.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("md_external_mapping")
public class MdExternalMappingEntity {
    @TableId(type=IdType.AUTO) private Long id;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    private String objectType;
    private String internalId;
    private String sourceSystem;
    private String externalId;
    private String externalLineId;
    private Long externalVersion;
    private String mappingStatus;
    private String conflictSummary;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    public String getObjectType(){return objectType;} public void setObjectType(String v){objectType=v;}
    public String getInternalId(){return internalId;} public void setInternalId(String v){internalId=v;}
    public String getSourceSystem(){return sourceSystem;} public void setSourceSystem(String v){sourceSystem=v;}
    public String getExternalId(){return externalId;} public void setExternalId(String v){externalId=v;}
    public String getExternalLineId(){return externalLineId;} public void setExternalLineId(String v){externalLineId=v;}
    public Long getExternalVersion(){return externalVersion;} public void setExternalVersion(Long v){externalVersion=v;}
    public String getMappingStatus(){return mappingStatus;} public void setMappingStatus(String v){mappingStatus=v;}
    public String getConflictSummary(){return conflictSummary;} public void setConflictSummary(String v){conflictSummary=v;}
    public LocalDateTime getLastSyncAt(){return lastSyncAt;} public void setLastSyncAt(LocalDateTime v){lastSyncAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
