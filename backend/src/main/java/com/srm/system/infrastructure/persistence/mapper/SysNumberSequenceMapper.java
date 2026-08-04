package com.srm.system.infrastructure.persistence.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.srm.system.infrastructure.persistence.entity.SysNumberSequenceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
@Mapper
public interface SysNumberSequenceMapper extends BaseMapper<SysNumberSequenceEntity> {
    @Update("""
            UPDATE sys_number_sequence
               SET current_sequence = current_sequence + 1,
                   updated_by = #{updatedBy},
                   updated_at = CURRENT_TIMESTAMP(6)
             WHERE rule_id = #{ruleId}
               AND org_id = #{orgId}
               AND period_key = #{periodKey}
            """)
    int increment(@Param("ruleId") Long ruleId,
                  @Param("orgId") Long orgId,
                  @Param("periodKey") String periodKey,
                  @Param("updatedBy") String updatedBy);

    @Select("""
            SELECT current_sequence
              FROM sys_number_sequence
             WHERE rule_id = #{ruleId}
               AND org_id = #{orgId}
               AND period_key = #{periodKey}
             FOR UPDATE
            """)
    Long selectCurrentForUpdate(@Param("ruleId") Long ruleId,
                                @Param("orgId") Long orgId,
                                @Param("periodKey") String periodKey);
}
