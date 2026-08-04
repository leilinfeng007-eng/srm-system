package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_dictionary_item")
public class SysDictionaryItemEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long dictId; private String itemCode; private String itemName;
    private Integer sortOrder; private String status; private String description;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getDictId(){return dictId;} public void setDictId(Long v){this.dictId=v;}
    public String getItemCode(){return itemCode;} public void setItemCode(String s){this.itemCode=s;}
    public String getItemName(){return itemName;} public void setItemName(String s){this.itemName=s;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){this.sortOrder=v;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getDescription(){return description;} public void setDescription(String s){this.description=s;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
}
