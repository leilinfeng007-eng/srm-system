package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_integration_job")
public class SysIntegrationJobEntity {
    @TableId(type=IdType.AUTO) private Long id;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    private String jobType;
    private String objectType;
    private String sourceId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public String getJobType(){return jobType;} public void setJobType(String v){jobType=v;}
    public String getObjectType(){return objectType;} public void setObjectType(String v){objectType=v;}
    public String getSourceId(){return sourceId;} public void setSourceId(String v){sourceId=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
