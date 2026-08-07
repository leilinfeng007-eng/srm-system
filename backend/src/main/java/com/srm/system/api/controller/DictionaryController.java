package com.srm.system.api.controller;
import com.srm.common.api.ApiResponse;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.api.request.DictionaryItemRequest;
import com.srm.system.api.request.DictionaryRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/dictionaries")
public class DictionaryController {
    private final SystemGovernanceFacade r;
    public DictionaryController(SystemGovernanceFacade r) { this.r = r; }
    @GetMapping @PreAuthorize("hasAuthority('system:dictionary:view')") public ApiResponse<?> list(@RequestParam(required=false) String keyword,@RequestParam(required=false) String status) { return ApiResponse.success(r.dictionaries(keyword,status)); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('system:dictionary:view')") public ApiResponse<?> get(@PathVariable Long id) { return ApiResponse.success(r.dictionary(id)); }
    @PostMapping @PreAuthorize("hasAuthority('system:dictionary:create')") public ApiResponse<?> create(@RequestBody DictionaryRequest b) { return ApiResponse.success(r.createDictionary(b.dictCode(),b.dictName(),b.description())); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('system:dictionary:update')") public ApiResponse<Void> update(@PathVariable Long id,@RequestBody DictionaryRequest b) { r.updateDictionary(id,b.dictName(),b.description()); return ApiResponse.success(); }
    @PostMapping("/{id}/enable") @PreAuthorize("hasAuthority('system:dictionary:enable')") public ApiResponse<Void> enable(@PathVariable Long id) { r.setDictionaryStatus(id,"ACTIVE"); return ApiResponse.success(); }
    @PostMapping("/{id}/disable") @PreAuthorize("hasAuthority('system:dictionary:disable')") public ApiResponse<Void> disable(@PathVariable Long id) { r.setDictionaryStatus(id,"INACTIVE"); return ApiResponse.success(); }
    @GetMapping("/{dictId}/items") @PreAuthorize("hasAuthority('system:dictionary:view')") public ApiResponse<?> items(@PathVariable Long dictId) { return ApiResponse.success(r.dictionaryItems(dictId)); }
    @PostMapping("/{dictId}/items") @PreAuthorize("hasAuthority('system:dictionary:create')") public ApiResponse<?> createItem(@PathVariable Long dictId,@RequestBody DictionaryItemRequest b) { return ApiResponse.success(r.createDictionaryItem(dictId,b.itemCode(),b.itemName(),b.sortOrder())); }
    @PutMapping("/{dictId}/items/{itemId}") @PreAuthorize("hasAuthority('system:dictionary:update')") public ApiResponse<Void> updateItem(@PathVariable Long dictId,@PathVariable Long itemId,@RequestBody DictionaryItemRequest b) { r.updateDictionaryItem(itemId,b.itemName(),b.sortOrder()); return ApiResponse.success(); }
    @PostMapping("/{dictId}/items/{itemId}/enable") @PreAuthorize("hasAuthority('system:dictionary:enable')") public ApiResponse<Void> enableItem(@PathVariable Long dictId,@PathVariable Long itemId) { r.setDictionaryItemStatus(itemId,"ACTIVE"); return ApiResponse.success(); }
    @PostMapping("/{dictId}/items/{itemId}/disable") @PreAuthorize("hasAuthority('system:dictionary:disable')") public ApiResponse<Void> disableItem(@PathVariable Long dictId,@PathVariable Long itemId) { r.setDictionaryItemStatus(itemId,"INACTIVE"); return ApiResponse.success(); }
}
