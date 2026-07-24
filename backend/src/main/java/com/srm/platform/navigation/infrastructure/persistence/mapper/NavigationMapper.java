package com.srm.platform.navigation.infrastructure.persistence.mapper;

import com.srm.platform.navigation.infrastructure.persistence.dto.NavigationMenuRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NavigationMapper {

    List<NavigationMenuRow> selectGrantedRows(@Param("userId") long userId);
}
