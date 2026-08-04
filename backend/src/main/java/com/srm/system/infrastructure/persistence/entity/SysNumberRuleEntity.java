package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_number_rule")
public class SysNumberRuleEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String ruleCode; private String ruleName; private String objectType;
    private String prefix; private String dateFormat; private Integer serialLength;
    private String resetCycle; private Boolean organizationDimension; private String status;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    @Version private Long version;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getRuleCode(){return ruleCode;} public void setRuleCode(String s){this.ruleCode=s;}
    public String getRuleName(){return ruleName;} public void setRuleName(String s){this.ruleName=s;}
    public String getObjectType(){return objectType;} public void setObjectType(String s){this.objectType=s;}
    public String getPrefix(){return prefix;} public void setPrefix(String s){this.prefix=s;}
    public String getDateFormat(){return dateFormat;} public void setDateFormat(String s){this.dateFormat=s;}
    public Integer getSerialLength(){return serialLength;} public void setSerialLength(Integer v){this.serialLength=v;}
    public String getResetCycle(){return resetCycle;} public void setResetCycle(String s){this.resetCycle=s;}
    public Boolean getOrganizationDimension(){return organizationDimension;} public void setOrganizationDimension(Boolean b){this.organizationDimension=b;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
}
