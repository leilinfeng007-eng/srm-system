package com.srm.masterdata.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("md_purchasing_organization")
public class MdPurchasingOrganizationEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String poCode; private String poName; private Long companyOrgId;
    private Long responsibleUserId; private String defaultCurrency; private String status;
    private String sourceType; private String sourceSystem; private String externalId;
    private Long externalVersion; private LocalDateTime lastSyncAt;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    @Version private Long version;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getPoCode(){return poCode;} public void setPoCode(String s){poCode=s;}
    public String getPoName(){return poName;} public void setPoName(String s){poName=s;}
    public Long getCompanyOrgId(){return companyOrgId;} public void setCompanyOrgId(Long v){companyOrgId=v;}
    public Long getResponsibleUserId(){return responsibleUserId;} public void setResponsibleUserId(Long v){responsibleUserId=v;}
    public String getDefaultCurrency(){return defaultCurrency;} public void setDefaultCurrency(String s){defaultCurrency=s;}
    public String getStatus(){return status;} public void setStatus(String s){status=s;}
    public String getSourceType(){return sourceType;} public void setSourceType(String s){sourceType=s;}
    public String getSourceSystem(){return sourceSystem;} public void setSourceSystem(String s){sourceSystem=s;}
    public String getExternalId(){return externalId;} public void setExternalId(String s){externalId=s;}
    public Long getExternalVersion(){return externalVersion;} public void setExternalVersion(Long v){externalVersion=v;}
    public LocalDateTime getLastSyncAt(){return lastSyncAt;} public void setLastSyncAt(LocalDateTime t){lastSyncAt=t;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
