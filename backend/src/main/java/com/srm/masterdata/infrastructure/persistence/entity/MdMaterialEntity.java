package com.srm.masterdata.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("md_material")
public class MdMaterialEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String materialCode; private String materialName; private String specification;
    private String materialType; private String baseUnit; private Long categoryId;
    private String materialVersion; private Boolean isCritical; private String status;
    private String sourceType; private String sourceSystem; private String externalId;
    private Long externalVersion; private LocalDateTime lastSyncAt;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    @Version private Long version;
        public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getMaterialCode(){return materialCode;} public void setMaterialCode(String v){materialCode=v;}
    public String getMaterialName(){return materialName;} public void setMaterialName(String v){materialName=v;}
    public String getSpecification(){return specification;} public void setSpecification(String v){specification=v;}
    public String getMaterialType(){return materialType;} public void setMaterialType(String v){materialType=v;}
    public String getBaseUnit(){return baseUnit;} public void setBaseUnit(String v){baseUnit=v;}
    public Long getCategoryId(){return categoryId;} public void setCategoryId(Long v){categoryId=v;}
    public String getMaterialVersion(){return materialVersion;} public void setMaterialVersion(String v){materialVersion=v;}
    public Boolean getIsCritical(){return isCritical;} public void setIsCritical(Boolean v){isCritical=v;}
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
