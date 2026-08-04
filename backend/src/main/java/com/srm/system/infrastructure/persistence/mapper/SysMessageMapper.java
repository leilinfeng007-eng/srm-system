package com.srm.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.system.infrastructure.persistence.entity.SysMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessageEntity> {
}
