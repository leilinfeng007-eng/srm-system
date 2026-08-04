package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_number_sequence")
public class SysNumberSequenceEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long ruleId; private Long orgId; private String periodKey;
    private Long currentSequence; private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getRuleId(){return ruleId;} public void setRuleId(Long v){this.ruleId=v;}
    public Long getOrgId(){return orgId;} public void setOrgId(Long v){this.orgId=v;}
    public String getPeriodKey(){return periodKey;} public void setPeriodKey(String s){this.periodKey=s;}
    public Long getCurrentSequence(){return currentSequence;} public void setCurrentSequence(Long v){this.currentSequence=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
}
