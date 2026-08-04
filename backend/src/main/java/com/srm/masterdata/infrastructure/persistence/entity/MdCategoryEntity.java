package com.srm.masterdata.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("md_category")
public class MdCategoryEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String categoryCode; private String categoryName; private Long parentId;
    private String path; private Integer level; private Long responsibleOrgId;
    private Long categoryManagerId; private String riskLevel; private String status;
    private String sourceType; private String sourceSystem; private String externalId;
    private Long externalVersion; private LocalDateTime lastSyncAt;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    private Long version;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getCategoryCode(){return categoryCode;} public void setCategoryCode(String v){categoryCode=v;}
    public String getCategoryName(){return categoryName;} public void setCategoryName(String v){categoryName=v;}
    public Long getParentId(){return parentId;} public void setParentId(Long v){parentId=v;}
    public String getPath(){return path;} public void setPath(String v){path=v;}
    public Integer getLevel(){return level;} public void setLevel(Integer v){level=v;}
    public Long getResponsibleOrgId(){return responsibleOrgId;} public void setResponsibleOrgId(Long v){responsibleOrgId=v;}
    public Long getCategoryManagerId(){return categoryManagerId;} public void setCategoryManagerId(Long v){categoryManagerId=v;}
    public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
    public String getSourceSystem(){return sourceSystem;} public void setSourceSystem(String v){sourceSystem=v;}
    public String getExternalId(){return externalId;} public void setExternalId(String v){externalId=v;}
    public Long getExternalVersion(){return externalVersion;} public void setExternalVersion(Long v){externalVersion=v;}
    public LocalDateTime getLastSyncAt(){return lastSyncAt;} public void setLastSyncAt(LocalDateTime v){lastSyncAt=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
