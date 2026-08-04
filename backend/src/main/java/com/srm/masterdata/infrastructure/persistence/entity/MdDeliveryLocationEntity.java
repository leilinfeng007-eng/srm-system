package com.srm.masterdata.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("md_delivery_location")
public class MdDeliveryLocationEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String locationCode; private String locationName; private Long plantId; private Long warehouseId;
    private String address; private String contactPerson; private String contactPhone; private Boolean appointmentRequired;
    private String status; private String sourceType; private String sourceSystem; private String externalId;
    private Long externalVersion; private LocalDateTime lastSyncAt;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    @Version private Long version;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getLocationCode(){return locationCode;} public void setLocationCode(String s){locationCode=s;}
    public String getLocationName(){return locationName;} public void setLocationName(String s){locationName=s;}
    public Long getPlantId(){return plantId;} public void setPlantId(Long v){plantId=v;}
    public Long getWarehouseId(){return warehouseId;} public void setWarehouseId(Long v){warehouseId=v;}
    public String getAddress(){return address;} public void setAddress(String s){address=s;}
    public String getContactPerson(){return contactPerson;} public void setContactPerson(String s){contactPerson=s;}
    public String getContactPhone(){return contactPhone;} public void setContactPhone(String s){contactPhone=s;}
    public Boolean getAppointmentRequired(){return appointmentRequired;} public void setAppointmentRequired(Boolean b){appointmentRequired=b;}
    public String getStatus(){return status;} public void setStatus(String s){status=s;}
    public String getSourceType(){return sourceType;} public void setSourceType(String s){sourceType=s;}
    public String getSourceSystem(){return sourceSystem;} public void setSourceSystem(String s){sourceSystem=s;}
    public String getExternalId(){return externalId;} public void setExternalId(String s){externalId=s;}
    public Long getExternalVersion(){return externalVersion;} public void setExternalVersion(Long v){externalVersion=v;}
    public LocalDateTime getLastSyncAt(){return lastSyncAt;} public void setLastSyncAt(LocalDateTime t){lastSyncAt=t;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
