package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_document_template")
public class SysDocumentTemplateEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String templateCode; private String templateName; private String purpose;
    private String domainCode; private Integer templateVersion; private Long attachmentId;
    private String status; private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt; @Version private Long version;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTemplateCode(){return templateCode;} public void setTemplateCode(String s){this.templateCode=s;}
    public String getTemplateName(){return templateName;} public void setTemplateName(String s){this.templateName=s;}
    public String getPurpose(){return purpose;} public void setPurpose(String s){this.purpose=s;}
    public String getDomainCode(){return domainCode;} public void setDomainCode(String s){this.domainCode=s;}
    public Integer getTemplateVersion(){return templateVersion;} public void setTemplateVersion(Integer v){this.templateVersion=v;}
    public Long getAttachmentId(){return attachmentId;} public void setAttachmentId(Long v){this.attachmentId=v;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
}
