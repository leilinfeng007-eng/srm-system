package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_attachment_version")
public class SysAttachmentVersionEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long attachmentId; private Integer version;
    private String fileName; private String storageKey;
    private String mimeType; private Long fileSize; private String fileSha256;
    private String createdBy; private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getAttachmentId(){return attachmentId;} public void setAttachmentId(Long v){this.attachmentId=v;}
    public Integer getVersion(){return version;} public void setVersion(Integer v){this.version=v;}
    public String getFileName(){return fileName;} public void setFileName(String s){this.fileName=s;}
    public String getStorageKey(){return storageKey;} public void setStorageKey(String s){this.storageKey=s;}
    public String getMimeType(){return mimeType;} public void setMimeType(String s){this.mimeType=s;}
    public Long getFileSize(){return fileSize;} public void setFileSize(Long v){this.fileSize=v;}
    public String getFileSha256(){return fileSha256;} public void setFileSha256(String s){this.fileSha256=s;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
}
