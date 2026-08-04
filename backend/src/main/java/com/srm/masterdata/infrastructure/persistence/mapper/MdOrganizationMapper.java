package com.srm.masterdata.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.masterdata.infrastructure.persistence.entity.MdOrganizationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MdOrganizationMapper extends BaseMapper<MdOrganizationEntity> {

    Long countActiveDepartments(@Param("organizationId") Long organizationId);

    Long countActivePositions(@Param("organizationId") Long organizationId);

    Long countActiveUsers(@Param("organizationId") Long organizationId);

    @Select("SELECT COUNT(*) FROM md_plant WHERE organization_id = #{organizationId} AND status = 'ACTIVE'")
    Long countActivePlants(@Param("organizationId") Long organizationId);

    @Select("SELECT COUNT(*) FROM md_warehouse WHERE organization_id = #{organizationId} AND status = 'ACTIVE'")
    Long countActiveWarehouses(@Param("organizationId") Long organizationId);
}
