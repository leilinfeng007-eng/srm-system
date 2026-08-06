package com.srm.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
interface SystemBaselineTestMapper {

    @Select("""
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE LOWER(table_name) LIKE 'sys_%'
            """)
    int countSystemTables();

    @Select("SELECT COUNT(*) FROM sys_role")
    int countRoles();

    @Select("SELECT COUNT(*) FROM sys_user")
    int countUsers();

    @Select("SELECT COUNT(DISTINCT role_code) FROM sys_role WHERE role_code IN (" +
            "'SUPER_ADMIN','SKELETON_VIEWER','SYSTEM_ADMIN','MASTER_DATA_ADMIN'," +
            "'PROCESS_ADMIN','INTERNAL_AUDITOR','INTERNAL_USER')")
    int countBootstrapRoles();

    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE action_code = #{actionCode}")
    int countOperations(String actionCode);
}
