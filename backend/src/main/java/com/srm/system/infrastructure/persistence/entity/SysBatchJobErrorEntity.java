package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
@TableName("sys_batch_job_error")
public class SysBatchJobErrorEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long jobId;
    @TableField("error_row") private Integer rowNumber; private String fieldName;
    private String errorCode; private String errorMessage; private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getJobId(){return jobId;} public void setJobId(Long v){this.jobId=v;}
    public Integer getRowNumber(){return rowNumber;} public void setRowNumber(Integer v){this.rowNumber=v;}
    public String getFieldName(){return fieldName;} public void setFieldName(String s){this.fieldName=s;}
    public String getErrorCode(){return errorCode;} public void setErrorCode(String s){this.errorCode=s;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String s){this.errorMessage=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
}
