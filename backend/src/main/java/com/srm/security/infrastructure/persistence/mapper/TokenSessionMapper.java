package com.srm.security.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.security.infrastructure.persistence.entity.SysRefreshTokenEntity;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TokenSessionMapper extends BaseMapper<SysRefreshTokenEntity> {

    int countActiveAccess(
            @Param("sessionId") long sessionId,
            @Param("userId") long userId,
            @Param("accessJti") String accessJti,
            @Param("now") Instant now);

    int revokeByUserId(@Param("userId") long userId, @Param("now") Instant now);
}
