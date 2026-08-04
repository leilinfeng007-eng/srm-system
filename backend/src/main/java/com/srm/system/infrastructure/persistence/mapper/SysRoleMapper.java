package com.srm.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.system.infrastructure.persistence.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {

    @Select("SELECT sr.role_code FROM sys_role sr INNER JOIN sys_user_role sur ON sr.id = sur.role_id WHERE sur.user_id = #{userId} AND sr.status = 'ACTIVE' AND sur.status = 'ACTIVE'")
    List<String> selectActiveRoleCodesByUserId(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId} AND status = 'ACTIVE'")
    long countActiveUsers(@Param("roleId") long roleId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId} AND status = 'ACTIVE' AND user_id != #{excludeUserId}")
    long countActiveUsersExcluding(@Param("roleId") long roleId, @Param("excludeUserId") long excludeUserId);

    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} AND status = 'ACTIVE'")
    Long findIdByRoleCode(@Param("roleCode") String roleCode);
}
