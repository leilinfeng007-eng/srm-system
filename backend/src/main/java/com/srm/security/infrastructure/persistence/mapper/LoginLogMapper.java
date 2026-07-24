package com.srm.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.security.infrastructure.persistence.entity.SysLoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<SysLoginLogEntity> {}
