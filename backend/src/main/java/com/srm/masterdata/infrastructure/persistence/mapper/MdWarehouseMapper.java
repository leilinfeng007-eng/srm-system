package com.srm.masterdata.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.masterdata.infrastructure.persistence.entity.MdWarehouseEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MdWarehouseMapper extends BaseMapper<MdWarehouseEntity> {
}
