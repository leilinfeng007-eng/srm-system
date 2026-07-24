package com.srm.workbench.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkbenchBaselineMapper {

    @Select("SELECT 1")
    int selectDatabaseProbe();
}
