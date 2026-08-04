package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_dictionary")
public class SysDictionaryEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String dictCode; private String dictName; private String status;
    private String description; private LocalDateTime effectiveFrom; private LocalDateTime effectiveTo;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    private Long version;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getDictCode(){return dictCode;} public void setDictCode(String s){this.dictCode=s;}
    public String getDictName(){return dictName;} public void setDictName(String s){this.dictName=s;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getDescription(){return description;} public void setDescription(String s){this.description=s;}
    public LocalDateTime getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(LocalDateTime t){this.effectiveFrom=t;}
    public LocalDateTime getEffectiveTo(){return effectiveTo;} public void setEffectiveTo(LocalDateTime t){this.effectiveTo=t;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
}
