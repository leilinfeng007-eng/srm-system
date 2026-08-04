package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisDictionaryRepository {
    private final SysDictionaryMapper dictMapper;
    private final SysDictionaryItemMapper itemMapper;

    public MybatisDictionaryRepository(SysDictionaryMapper dictMapper, SysDictionaryItemMapper itemMapper) {
        this.dictMapper = dictMapper;
        this.itemMapper = itemMapper;
    }

    public List<SysDictionaryEntity> listDictionaries() {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDictionaryEntity>()
                .orderByAsc(SysDictionaryEntity::getDictCode));
    }

    public SysDictionaryEntity getDictionary(Long id) {
        var d = dictMapper.selectById(id);
        if (d == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return d;
    }

    @Transactional
    public SysDictionaryEntity createDict(String dictCode, String dictName, String description) {
        if (dictMapper.selectCount(new LambdaQueryWrapper<SysDictionaryEntity>()
                .eq(SysDictionaryEntity::getDictCode, dictCode)) > 0)
            throw new BusinessException(ErrorCode.CONFLICT, "Dictionary code already exists");
        var e = new SysDictionaryEntity();
        e.setDictCode(dictCode); e.setDictName(dictName); e.setDescription(description);
        e.setStatus("ACTIVE"); e.setCreatedBy(actor()); e.setUpdatedBy(actor());
        dictMapper.insert(e);
        return e;
    }

    @Transactional
    public void updateDict(Long id, String dictName, String description) {
        var e = dictMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (dictName != null) e.setDictName(dictName);
        if (description != null) e.setDescription(description);
        e.setUpdatedBy(actor());
        dictMapper.updateById(e);
    }

    @Transactional
    public void toggleDict(Long id, String status) {
        var e = dictMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        e.setStatus(status); e.setUpdatedBy(actor());
        dictMapper.updateById(e);
    }

    public List<SysDictionaryItemEntity> listItems(Long dictId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictionaryItemEntity>()
                .eq(SysDictionaryItemEntity::getDictId, dictId)
                .orderByAsc(SysDictionaryItemEntity::getSortOrder));
    }

    @Transactional
    public SysDictionaryItemEntity createItem(Long dictId, String itemCode, String itemName, Integer sortOrder) {
        if (itemMapper.selectCount(new LambdaQueryWrapper<SysDictionaryItemEntity>()
                .eq(SysDictionaryItemEntity::getDictId, dictId)
                .eq(SysDictionaryItemEntity::getItemCode, itemCode)) > 0)
            throw new BusinessException(ErrorCode.CONFLICT, "Item code already exists in this dictionary");
        var e = new SysDictionaryItemEntity();
        e.setDictId(dictId); e.setItemCode(itemCode); e.setItemName(itemName);
        e.setSortOrder(sortOrder != null ? sortOrder : 0); e.setStatus("ACTIVE");
        e.setCreatedBy(actor()); e.setUpdatedBy(actor());
        itemMapper.insert(e);
        return e;
    }

    @Transactional
    public void updateItem(Long id, String itemName, Integer sortOrder) {
        var e = itemMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (itemName != null) e.setItemName(itemName);
        if (sortOrder != null) e.setSortOrder(sortOrder);
        e.setUpdatedBy(actor());
        itemMapper.updateById(e);
    }

    @Transactional
    public void toggleItem(Long id, String status) {
        var e = itemMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        e.setStatus(status); e.setUpdatedBy(actor());
        itemMapper.updateById(e);
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
