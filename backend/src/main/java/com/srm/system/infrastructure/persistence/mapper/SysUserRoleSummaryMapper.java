package com.srm.system.infrastructure.persistence.mapper;

import com.srm.system.domain.model.UserRoleSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserRoleSummaryMapper {

    @Select("""
            SELECT r.id AS role_id, r.role_name, r.status AS role_status
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.status = 'ACTIVE'
            ORDER BY r.role_code
            """)
    List<UserRoleSummary> selectByUserId(@Param("userId") long userId);
}
