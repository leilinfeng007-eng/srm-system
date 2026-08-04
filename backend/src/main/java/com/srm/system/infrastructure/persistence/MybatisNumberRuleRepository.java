package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisNumberRuleRepository {
    private final SysNumberRuleMapper ruleMapper;
    private final SysNumberSequenceMapper seqMapper;

    public MybatisNumberRuleRepository(SysNumberRuleMapper ruleMapper, SysNumberSequenceMapper seqMapper) {
        this.ruleMapper = ruleMapper;
        this.seqMapper = seqMapper;
    }

    public List<SysNumberRuleEntity> listAll() {
        return ruleMapper.selectList(null);
    }

    @Transactional
    public SysNumberRuleEntity createRule(String ruleCode, String ruleName, String objectType,
                                           String prefix, String dateFormat, Integer serialLength,
                                           String resetCycle, Boolean orgDimension) {
        if (ruleMapper.selectCount(new LambdaQueryWrapper<SysNumberRuleEntity>()
                .eq(SysNumberRuleEntity::getRuleCode, ruleCode)) > 0)
            throw new BusinessException(ErrorCode.CONFLICT, "Rule code already exists");
        var e = new SysNumberRuleEntity();
        e.setRuleCode(ruleCode); e.setRuleName(ruleName); e.setObjectType(objectType);
        e.setPrefix(prefix); e.setDateFormat(dateFormat);
        e.setSerialLength(serialLength != null ? serialLength : 5);
        e.setResetCycle(resetCycle != null ? resetCycle : "NONE");
        e.setOrganizationDimension(Boolean.TRUE.equals(orgDimension));
        e.setStatus("ACTIVE"); e.setCreatedBy(actor()); e.setUpdatedBy(actor());
        ruleMapper.insert(e);
        return e;
    }

    @Transactional
    public void updateRule(Long id, String ruleName, String prefix, String dateFormat,
                            Integer serialLength, String resetCycle) {
        var e = ruleMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (ruleName != null) e.setRuleName(ruleName);
        if (prefix != null) e.setPrefix(prefix);
        if (dateFormat != null) e.setDateFormat(dateFormat);
        if (serialLength != null) e.setSerialLength(serialLength);
        if (resetCycle != null) e.setResetCycle(resetCycle);
        e.setUpdatedBy(actor());
        ruleMapper.updateById(e);
    }

    @Transactional
    public void toggleRule(Long id, String status) {
        var e = ruleMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        e.setStatus(status); e.setUpdatedBy(actor());
        ruleMapper.updateById(e);
    }

    @Transactional
    public String generateNumber(String ruleCode) {
        return generateNumber(ruleCode, null);
    }

    @Transactional
    public String generateNumber(String ruleCode, Long orgId) {
        var rule = ruleMapper.selectOne(new LambdaQueryWrapper<SysNumberRuleEntity>()
                .eq(SysNumberRuleEntity::getRuleCode, ruleCode)
                .eq(SysNumberRuleEntity::getStatus, "ACTIVE"));
        if (rule == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Number rule not found or inactive");

        String periodKey = computePeriodKey(rule.getResetCycle());
        Long effectiveOrgId = Boolean.TRUE.equals(rule.getOrganizationDimension()) ? orgId : 0L;

        String currentActor = actor();
        long nextSeq;
        if (seqMapper.increment(rule.getId(), effectiveOrgId, periodKey, currentActor) == 1) {
            nextSeq = seqMapper.selectCurrentForUpdate(rule.getId(), effectiveOrgId, periodKey);
        } else {
            var seq = new SysNumberSequenceEntity();
            seq.setRuleId(rule.getId()); seq.setOrgId(effectiveOrgId);
            seq.setPeriodKey(periodKey); seq.setCurrentSequence(1L);
            seq.setCreatedBy(currentActor); seq.setUpdatedBy(currentActor);
            try {
                seqMapper.insert(seq);
                nextSeq = 1L;
            } catch (DuplicateKeyException concurrentInsert) {
                if (seqMapper.increment(rule.getId(), effectiveOrgId, periodKey, currentActor) != 1) {
                    throw concurrentInsert;
                }
                nextSeq = seqMapper.selectCurrentForUpdate(rule.getId(), effectiveOrgId, periodKey);
            }
        }

        String datePart = "";
        if (rule.getDateFormat() != null && !rule.getDateFormat().isEmpty()) {
            try {
                datePart = LocalDate.now().format(DateTimeFormatter.ofPattern(rule.getDateFormat()));
            } catch (Exception ignored) {}
        }
        String seqPart = String.format("%0" + rule.getSerialLength() + "d", nextSeq);
        return rule.getPrefix() + datePart + seqPart;
    }

    private String computePeriodKey(String resetCycle) {
        return switch (resetCycle != null ? resetCycle : "NONE") {
            case "DAY" -> LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            case "MONTH" -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "YEAR" -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
            default -> "FIXED";
        };
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
