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

    @Select("""
            SELECT COUNT(*)
            FROM sys_user_role ur
            JOIN sys_user u ON u.id = ur.user_id
            WHERE ur.role_id = (SELECT id FROM sys_role WHERE role_code = #{roleCode} AND status = 'ACTIVE')
              AND ur.status = 'ACTIVE'
              AND u.status = 'ACTIVE'
            """)
    long countActiveUsersByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT id FROM sys_user_role WHERE role_id = #{roleId} AND status = 'ACTIVE' FOR UPDATE")
    List<Long> lockActiveAssignments(@Param("roleId") long roleId);

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
              AND p.enabled = TRUE
            """)
    List<String> selectPermissionCodesByRoleId(@Param("roleId") long roleId);

    @Select("""
            SELECT COUNT(DISTINCT ur.user_id)
            FROM sys_user_role ur
            JOIN sys_user u ON u.id = ur.user_id
            WHERE ur.role_id = #{roleId}
              AND ur.status = 'ACTIVE'
              AND u.status = 'ACTIVE'
              AND NOT EXISTS (
                SELECT 1 FROM sys_user_role ur2
                JOIN sys_role r2 ON r2.id = ur2.role_id
                WHERE ur2.user_id = ur.user_id
                  AND ur2.role_id != #{roleId}
                  AND ur2.status = 'ACTIVE'
                  AND r2.status = 'ACTIVE'
              )
            """)
    long countUsersLosingAllRoles(@Param("roleId") long roleId);
}
