package com.srm.masterdata.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@TableName("md_tax_code")
public class MdTaxCodeEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String taxCode; private String taxName; private String country;
    private BigDecimal taxRate; private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo; private String status;
    private String sourceType; private String sourceSystem; private String externalId;
    private Long externalVersion; private LocalDateTime lastSyncAt;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    private Long version;
        public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getTaxCode(){return taxCode;} public void setTaxCode(String v){taxCode=v;}
    public String getTaxName(){return taxName;} public void setTaxName(String v){taxName=v;}
    public String getCountry(){return country;} public void setCountry(String v){country=v;}
    public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal v){taxRate=v;}
    public LocalDateTime getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(LocalDateTime v){effectiveFrom=v;}
    public LocalDateTime getEffectiveTo(){return effectiveTo;} public void setEffectiveTo(LocalDateTime v){effectiveTo=v;}
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
