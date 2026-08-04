package com.srm.masterdata.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.masterdata.application.command.MasterDataCommand;
import com.srm.masterdata.application.service.MasterDataAdministration;
import com.srm.masterdata.application.service.MasterDataAdministration.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data")
public class MasterDataController {
    private final MasterDataAdministration service;
    public MasterDataController(MasterDataAdministration service){this.service=service;}

    @GetMapping("/purchasing-organizations") @PreAuthorize("hasAuthority('masterdata:purchasing-organization:view')") public ApiResponse<?> listPO(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status){return ApiResponse.success(service.list(Resource.PURCHASING_ORGANIZATION,page,size,keyword,status,null));}
    @GetMapping("/purchasing-organizations/{id}") @PreAuthorize("hasAuthority('masterdata:purchasing-organization:view')") public ApiResponse<?> getPO(@PathVariable Long id){return ApiResponse.success(service.get(Resource.PURCHASING_ORGANIZATION,id));}
    @PostMapping("/purchasing-organizations") @PreAuthorize("hasAuthority('masterdata:purchasing-organization:create')") public ApiResponse<?> createPO(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.PURCHASING_ORGANIZATION,c));}
    @PutMapping("/purchasing-organizations/{id}") @PreAuthorize("hasAuthority('masterdata:purchasing-organization:update')") public ApiResponse<Void> updatePO(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.PURCHASING_ORGANIZATION,id,c);return ApiResponse.success();}
    @PostMapping("/purchasing-organizations/{id}/enable") @PreAuthorize("hasAuthority('masterdata:purchasing-organization:enable')") public ApiResponse<Void> enablePO(@PathVariable Long id){service.setStatus(Resource.PURCHASING_ORGANIZATION,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/purchasing-organizations/{id}/disable") @PreAuthorize("hasAuthority('masterdata:purchasing-organization:disable')") public ApiResponse<Void> disablePO(@PathVariable Long id){service.setStatus(Resource.PURCHASING_ORGANIZATION,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/delivery-locations") @PreAuthorize("hasAuthority('masterdata:delivery-location:view')") public ApiResponse<?> listDL(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status){return ApiResponse.success(service.list(Resource.DELIVERY_LOCATION,page,size,keyword,status,null));}
    @GetMapping("/delivery-locations/{id}") @PreAuthorize("hasAuthority('masterdata:delivery-location:view')") public ApiResponse<?> getDL(@PathVariable Long id){return ApiResponse.success(service.get(Resource.DELIVERY_LOCATION,id));}
    @PostMapping("/delivery-locations") @PreAuthorize("hasAuthority('masterdata:delivery-location:create')") public ApiResponse<?> createDL(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.DELIVERY_LOCATION,c));}
    @PutMapping("/delivery-locations/{id}") @PreAuthorize("hasAuthority('masterdata:delivery-location:update')") public ApiResponse<Void> updateDL(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.DELIVERY_LOCATION,id,c);return ApiResponse.success();}
    @PostMapping("/delivery-locations/{id}/enable") @PreAuthorize("hasAuthority('masterdata:delivery-location:enable')") public ApiResponse<Void> enableDL(@PathVariable Long id){service.setStatus(Resource.DELIVERY_LOCATION,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/delivery-locations/{id}/disable") @PreAuthorize("hasAuthority('masterdata:delivery-location:disable')") public ApiResponse<Void> disableDL(@PathVariable Long id){service.setStatus(Resource.DELIVERY_LOCATION,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/categories") @PreAuthorize("hasAuthority('masterdata:category:view')") public ApiResponse<?> listCat(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status){return ApiResponse.success(service.list(Resource.CATEGORY,page,size,keyword,status,null));}
    @GetMapping("/categories/{id}") @PreAuthorize("hasAuthority('masterdata:category:view')") public ApiResponse<?> getCat(@PathVariable Long id){return ApiResponse.success(service.get(Resource.CATEGORY,id));}
    @PostMapping("/categories") @PreAuthorize("hasAuthority('masterdata:category:create')") public ApiResponse<?> createCat(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.CATEGORY,c));}
    @PutMapping("/categories/{id}") @PreAuthorize("hasAuthority('masterdata:category:update')") public ApiResponse<Void> updateCat(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.CATEGORY,id,c);return ApiResponse.success();}
    @PostMapping("/categories/{id}/enable") @PreAuthorize("hasAuthority('masterdata:category:enable')") public ApiResponse<Void> enableCat(@PathVariable Long id){service.setStatus(Resource.CATEGORY,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/categories/{id}/disable") @PreAuthorize("hasAuthority('masterdata:category:disable')") public ApiResponse<Void> disableCat(@PathVariable Long id){service.setStatus(Resource.CATEGORY,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/units") @PreAuthorize("hasAuthority('masterdata:unit:view')") public ApiResponse<?> listUnit(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.success(service.list(Resource.UNIT,page,size,null));}
    @GetMapping("/units/{id}") @PreAuthorize("hasAuthority('masterdata:unit:view')") public ApiResponse<?> getUnit(@PathVariable Long id){return ApiResponse.success(service.get(Resource.UNIT,id));}
    @PostMapping("/units") @PreAuthorize("hasAuthority('masterdata:unit:create')") public ApiResponse<?> createUnit(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.UNIT,c));}
    @PutMapping("/units/{id}") @PreAuthorize("hasAuthority('masterdata:unit:update')") public ApiResponse<Void> updateUnit(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.UNIT,id,c);return ApiResponse.success();}
    @PostMapping("/units/{id}/enable") @PreAuthorize("hasAuthority('masterdata:unit:enable')") public ApiResponse<Void> enableUnit(@PathVariable Long id){service.setStatus(Resource.UNIT,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/units/{id}/disable") @PreAuthorize("hasAuthority('masterdata:unit:disable')") public ApiResponse<Void> disableUnit(@PathVariable Long id){service.setStatus(Resource.UNIT,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/currencies") @PreAuthorize("hasAuthority('masterdata:currency:view')") public ApiResponse<?> listCurr(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.success(service.list(Resource.CURRENCY,page,size,null));}
    @GetMapping("/currencies/{id}") @PreAuthorize("hasAuthority('masterdata:currency:view')") public ApiResponse<?> getCurr(@PathVariable Long id){return ApiResponse.success(service.get(Resource.CURRENCY,id));}
    @PostMapping("/currencies") @PreAuthorize("hasAuthority('masterdata:currency:create')") public ApiResponse<?> createCurr(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.CURRENCY,c));}
    @PutMapping("/currencies/{id}") @PreAuthorize("hasAuthority('masterdata:currency:update')") public ApiResponse<Void> updateCurr(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.CURRENCY,id,c);return ApiResponse.success();}
    @PostMapping("/currencies/{id}/enable") @PreAuthorize("hasAuthority('masterdata:currency:enable')") public ApiResponse<Void> enableCurr(@PathVariable Long id){service.setStatus(Resource.CURRENCY,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/currencies/{id}/disable") @PreAuthorize("hasAuthority('masterdata:currency:disable')") public ApiResponse<Void> disableCurr(@PathVariable Long id){service.setStatus(Resource.CURRENCY,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/tax-codes") @PreAuthorize("hasAuthority('masterdata:tax-code:view')") public ApiResponse<?> listTax(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.success(service.list(Resource.TAX_CODE,page,size,null));}
    @GetMapping("/tax-codes/{id}") @PreAuthorize("hasAuthority('masterdata:tax-code:view')") public ApiResponse<?> getTax(@PathVariable Long id){return ApiResponse.success(service.get(Resource.TAX_CODE,id));}
    @PostMapping("/tax-codes") @PreAuthorize("hasAuthority('masterdata:tax-code:create')") public ApiResponse<?> createTax(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.TAX_CODE,c));}
    @PutMapping("/tax-codes/{id}") @PreAuthorize("hasAuthority('masterdata:tax-code:update')") public ApiResponse<Void> updateTax(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.TAX_CODE,id,c);return ApiResponse.success();}
    @PostMapping("/tax-codes/{id}/enable") @PreAuthorize("hasAuthority('masterdata:tax-code:enable')") public ApiResponse<Void> enableTax(@PathVariable Long id){service.setStatus(Resource.TAX_CODE,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/tax-codes/{id}/disable") @PreAuthorize("hasAuthority('masterdata:tax-code:disable')") public ApiResponse<Void> disableTax(@PathVariable Long id){service.setStatus(Resource.TAX_CODE,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/materials") @PreAuthorize("hasAuthority('masterdata:material:view')") public ApiResponse<?> listMat(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status){return ApiResponse.success(service.list(Resource.MATERIAL,page,size,keyword,status,null));}
    @GetMapping("/materials/{id}") @PreAuthorize("hasAuthority('masterdata:material:view')") public ApiResponse<?> getMat(@PathVariable Long id){return ApiResponse.success(service.get(Resource.MATERIAL,id));}
    @PostMapping("/materials") @PreAuthorize("hasAuthority('masterdata:material:create')") public ApiResponse<?> createMat(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.MATERIAL,c));}
    @PutMapping("/materials/{id}") @PreAuthorize("hasAuthority('masterdata:material:update')") public ApiResponse<Void> updateMat(@PathVariable Long id,@RequestBody MasterDataCommand c){service.update(Resource.MATERIAL,id,c);return ApiResponse.success();}
    @PostMapping("/materials/{id}/enable") @PreAuthorize("hasAuthority('masterdata:material:enable')") public ApiResponse<Void> enableMat(@PathVariable Long id){service.setStatus(Resource.MATERIAL,id,"ACTIVE");return ApiResponse.success();}
    @PostMapping("/materials/{id}/disable") @PreAuthorize("hasAuthority('masterdata:material:disable')") public ApiResponse<Void> disableMat(@PathVariable Long id){service.setStatus(Resource.MATERIAL,id,"INACTIVE");return ApiResponse.success();}

    @GetMapping("/external-mappings") @PreAuthorize("hasAuthority('masterdata:external-mapping:view')") public ApiResponse<?> listMap(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String sourceSystem,@RequestParam(required=false)String status){return ApiResponse.success(service.list(Resource.EXTERNAL_MAPPING,page,size,null,status,sourceSystem));}
    @GetMapping("/external-mappings/{id}") @PreAuthorize("hasAuthority('masterdata:external-mapping:view')") public ApiResponse<?> getMap(@PathVariable Long id){return ApiResponse.success(service.get(Resource.EXTERNAL_MAPPING,id));}
    @PostMapping("/external-mappings") @PreAuthorize("hasAuthority('masterdata:external-mapping:create')") public ApiResponse<?> createMap(@RequestBody MasterDataCommand c){return ApiResponse.success(service.create(Resource.EXTERNAL_MAPPING,c));}
}
