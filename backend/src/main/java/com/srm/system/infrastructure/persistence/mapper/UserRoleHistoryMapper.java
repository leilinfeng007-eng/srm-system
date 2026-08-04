package com.srm.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.system.infrastructure.persistence.entity.SysUserRoleHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleHistoryMapper extends BaseMapper<SysUserRoleHistoryEntity> {
}
