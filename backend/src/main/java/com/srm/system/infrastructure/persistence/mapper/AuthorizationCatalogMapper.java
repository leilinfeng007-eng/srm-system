package com.srm.system.infrastructure.persistence.mapper;

import com.srm.system.domain.model.EffectivePermissionGrant;
import com.srm.system.domain.model.MenuCatalogItem;
import com.srm.system.domain.model.PermissionCatalogItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthorizationCatalogMapper {
    @Select("""
            SELECT id, permission_code, domain_code, resource_code, action_code,
                   description, enabled
            FROM sys_permission
            ORDER BY domain_code, resource_code, action_code, id
            LIMIT #{offset}, #{limit}
            """)
    List<PermissionCatalogItem> findPermissions(@Param("offset") int offset,
                                                 @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_permission")
    long countPermissions();

    @Select("""
            SELECT id, parent_id, menu_code, label, route, component_key,
                   permission_code, sort_order
            FROM sys_menu
            WHERE enabled = TRUE AND visible = TRUE AND phase <= 1
            ORDER BY sort_order, id
            """)
    List<MenuCatalogItem> findEnabledMenus();

    @Select("""
            SELECT DISTINCT permission.permission_code, role.role_code,
                   policy.domain_code, policy.dimension_code,
                   policy.scope_type, policy.operation_mode
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id AND role.status = 'ACTIVE'
            JOIN sys_role_permission role_permission ON role_permission.role_id = role.id
            JOIN sys_permission permission ON permission.id = role_permission.permission_id
                                           AND permission.enabled = TRUE
            LEFT JOIN sys_role_data_policy policy ON policy.role_id = role.id
                                                  AND policy.status = 'ACTIVE'
            WHERE user_role.user_id = #{userId} AND user_role.status = 'ACTIVE'
            ORDER BY permission.permission_code, role.role_code,
                     policy.domain_code, policy.dimension_code
            """)
    List<EffectivePermissionGrant> findEffectivePermissionGrants(@Param("userId") Long userId);
}
