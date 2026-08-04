package com.srm.system.infrastructure.persistence.mapper;

import com.srm.system.infrastructure.persistence.dto.DataScopeGrantRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataScopeGrantMapper {

    List<DataScopeGrantRow> selectGrants(
            @Param("userId") long userId,
            @Param("permissionCode") String permissionCode,
            @Param("domainCode") String domainCode,
            @Param("dimensionCode") String dimensionCode,
            @Param("writeOperation") boolean writeOperation);
}
