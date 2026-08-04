package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_parameter")
public class SysParameterEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String paramCode; private String paramName; private String paramType;
    private String defaultValue; private String validationRule; private Boolean approvalRequired;
    private String description; private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt; @Version private Long version;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getParamCode(){return paramCode;} public void setParamCode(String s){this.paramCode=s;}
    public String getParamName(){return paramName;} public void setParamName(String s){this.paramName=s;}
    public String getParamType(){return paramType;} public void setParamType(String s){this.paramType=s;}
    public String getDefaultValue(){return defaultValue;} public void setDefaultValue(String s){this.defaultValue=s;}
    public String getValidationRule(){return validationRule;} public void setValidationRule(String s){this.validationRule=s;}
    public Boolean getApprovalRequired(){return approvalRequired;} public void setApprovalRequired(Boolean b){this.approvalRequired=b;}
    public String getDescription(){return description;} public void setDescription(String s){this.description=s;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
}
