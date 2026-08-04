package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_attachment")
public class SysAttachmentEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String fileName; private String originalName; private String storageKey;
    private String mimeType; private Long fileSize; private String fileSha256;
    private String ownerType; private String ownerId; private String scanStatus; private String status;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    private Long version;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getFileName(){return fileName;} public void setFileName(String s){this.fileName=s;}
    public String getOriginalName(){return originalName;} public void setOriginalName(String s){this.originalName=s;}
    public String getStorageKey(){return storageKey;} public void setStorageKey(String s){this.storageKey=s;}
    public String getMimeType(){return mimeType;} public void setMimeType(String s){this.mimeType=s;}
    public Long getFileSize(){return fileSize;} public void setFileSize(Long v){this.fileSize=v;}
    public String getFileSha256(){return fileSha256;} public void setFileSha256(String s){this.fileSha256=s;}
    public String getOwnerType(){return ownerType;} public void setOwnerType(String s){this.ownerType=s;}
    public String getOwnerId(){return ownerId;} public void setOwnerId(String s){this.ownerId=s;}
    public String getScanStatus(){return scanStatus;} public void setScanStatus(String s){this.scanStatus=s;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
}
