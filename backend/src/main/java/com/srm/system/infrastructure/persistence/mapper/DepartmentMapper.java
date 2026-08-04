package com.srm.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.system.infrastructure.persistence.entity.SysDepartmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepartmentMapper extends BaseMapper<SysDepartmentEntity> {

    Long countActivePositions(@Param("departmentId") Long departmentId);

    Long countActiveUsers(@Param("departmentId") Long departmentId);
}
