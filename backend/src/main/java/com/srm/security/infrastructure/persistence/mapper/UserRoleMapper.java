package com.srm.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.security.infrastructure.persistence.entity.SysUserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper extends BaseMapper<SysUserRoleEntity> {

    Long selectActiveRoleId(@Param("roleCode") String roleCode);

    long countAssignment(@Param("userId") long userId, @Param("roleId") long roleId);
}
