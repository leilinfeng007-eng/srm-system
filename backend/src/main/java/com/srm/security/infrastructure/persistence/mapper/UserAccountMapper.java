package com.srm.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.security.infrastructure.persistence.entity.SysUserEntity;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper extends BaseMapper<SysUserEntity> {

    List<String> selectActiveRoles(@Param("userId") long userId);

    List<String> selectEnabledPermissions(@Param("userId") long userId);

    int updateLogin(
            @Param("userId") long userId,
            @Param("loginAt") Instant loginAt,
            @Param("actor") String actor);

    int updatePassword(
            @Param("userId") long userId,
            @Param("passwordHash") String passwordHash,
            @Param("mustChange") boolean mustChange,
            @Param("actor") String actor);
}
