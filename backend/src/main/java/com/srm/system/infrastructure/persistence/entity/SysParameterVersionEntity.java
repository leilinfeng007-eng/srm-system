package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_parameter_version")
public class SysParameterVersionEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long paramId; private Integer version; private String paramValue;
    private String status; private LocalDateTime effectiveFrom;
    private String publishedBy; private LocalDateTime publishedAt;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getParamId(){return paramId;} public void setParamId(Long v){this.paramId=v;}
    public Integer getVersion(){return version;} public void setVersion(Integer v){this.version=v;}
    public String getParamValue(){return paramValue;} public void setParamValue(String s){this.paramValue=s;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public LocalDateTime getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(LocalDateTime t){this.effectiveFrom=t;}
    public String getPublishedBy(){return publishedBy;} public void setPublishedBy(String s){this.publishedBy=s;}
    public LocalDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(LocalDateTime t){this.publishedAt=t;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
}
