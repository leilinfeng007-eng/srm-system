package com.srm.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.security.infrastructure.persistence.entity.SysOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<SysOperationLogEntity> {}
