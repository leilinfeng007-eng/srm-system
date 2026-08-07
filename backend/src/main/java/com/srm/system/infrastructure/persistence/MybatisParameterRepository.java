package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisParameterRepository implements com.srm.system.domain.repository.ParameterVersionRepository {
    private final SysParameterMapper paramMapper;
    private final SysParameterVersionMapper versionMapper;
    private final MybatisApprovalRepository approvalRepo;

    public MybatisParameterRepository(SysParameterMapper paramMapper,
                             SysParameterVersionMapper versionMapper,
                             MybatisApprovalRepository approvalRepo) {
        this.paramMapper = paramMapper;
        this.versionMapper = versionMapper;
        this.approvalRepo = approvalRepo;
    }

    public List<SysParameterEntity> listAll() {
        return paramMapper.selectList(null);
    }

    public SysParameterEntity getParam(Long id) {
        var p = paramMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return p;
    }

    public SysParameterEntity getByCode(String code) {
        var p = paramMapper.selectOne(new LambdaQueryWrapper<SysParameterEntity>()
                .eq(SysParameterEntity::getParamCode, code));
        if (p == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return p;
    }

    public List<SysParameterVersionEntity> listVersions(Long paramId) {
        return versionMapper.selectList(new LambdaQueryWrapper<SysParameterVersionEntity>()
                .eq(SysParameterVersionEntity::getParamId, paramId)
                .orderByDesc(SysParameterVersionEntity::getVersion));
    }

    public SysParameterVersionEntity getActiveVersion(Long paramId) {
        return versionMapper.selectOne(new LambdaQueryWrapper<SysParameterVersionEntity>()
                .eq(SysParameterVersionEntity::getParamId, paramId)
                .eq(SysParameterVersionEntity::getStatus, "ACTIVE"));
    }

    @Transactional
    public SysParameterEntity createParam(String paramCode, String paramName, String paramType,
                                            String defaultValue, String validationRule,
                                            Boolean approvalRequired, String description) {
        if (paramMapper.selectCount(new LambdaQueryWrapper<SysParameterEntity>()
                .eq(SysParameterEntity::getParamCode, paramCode)) > 0)
            throw new BusinessException(ErrorCode.CONFLICT, "Parameter code already exists");
        String normalizedType = validateTypeAndValue(paramType, defaultValue);
        var e = new SysParameterEntity();
        e.setParamCode(paramCode); e.setParamName(paramName); e.setParamType(normalizedType);
        e.setDefaultValue(defaultValue); e.setValidationRule(validationRule);
        e.setApprovalRequired(approvalRequired != null && approvalRequired);
        e.setDescription(description); e.setCreatedBy(actor()); e.setUpdatedBy(actor());
        paramMapper.insert(e);
        return e;
    }

    @Transactional
    public void updateParam(Long id, String paramName, String paramType, String defaultValue,
                            String validationRule, Boolean approvalRequired, String description) {
        var e = paramMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (paramName != null && paramName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Parameter name is required");
        }
        String type = paramType != null ? validateTypeAndValue(paramType,
                defaultValue != null ? defaultValue : e.getDefaultValue()) : e.getParamType();
        if (paramType != null) e.setParamType(type);
        if (paramName != null) e.setParamName(paramName);
        if (defaultValue != null) {
            validateValue(type, defaultValue);
            e.setDefaultValue(defaultValue);
        }
        if (validationRule != null) e.setValidationRule(validationRule);
        if (approvalRequired != null) e.setApprovalRequired(approvalRequired);
        if (description != null) e.setDescription(description);
        e.setUpdatedBy(actor());
        paramMapper.updateById(e);
    }

    private String validateTypeAndValue(String paramType, String value) {
        String type = paramType == null ? "STRING" : paramType.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE").contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported parameter type: " + paramType);
        }
        validateValue(type, value);
        return type;
    }

    public void validateValue(String paramType, String value) {
        if (value == null || value.isBlank()) return;
        String type = paramType == null ? "STRING" : paramType.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            switch (type) {
                case "INTEGER" -> Long.parseLong(value.trim());
                case "DECIMAL" -> new java.math.BigDecimal(value.trim());
                case "BOOLEAN" -> {
                    String v = value.trim();
                    if (!"true".equalsIgnoreCase(v) && !"false".equalsIgnoreCase(v)) {
                        throw new NumberFormatException("not a boolean");
                    }
                }
                case "DATE" -> java.time.LocalDate.parse(value.trim());
                default -> { }
            }
        } catch (RuntimeException invalid) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Value '" + value + "' does not match parameter type " + type);
        }
    }

    @Transactional
    public SysParameterVersionEntity createVersion(Long paramId, String paramValue) {
        var param = paramMapper.selectById(paramId);
        if (param == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        validateValue(param.getParamType(), paramValue);

        int nextVer = 1;
        var maxVer = versionMapper.selectOne(new LambdaQueryWrapper<SysParameterVersionEntity>()
                .eq(SysParameterVersionEntity::getParamId, paramId)
                .orderByDesc(SysParameterVersionEntity::getVersion));
        if (maxVer != null) nextVer = maxVer.getVersion() + 1;

        String initialStatus = "DRAFT";

        var ver = new SysParameterVersionEntity();
        ver.setParamId(paramId); ver.setVersion(nextVer); ver.setParamValue(paramValue);
        ver.setStatus(initialStatus); ver.setCreatedBy(actor()); ver.setUpdatedBy(actor());
        versionMapper.insert(ver);

        if (!Boolean.TRUE.equals(param.getApprovalRequired())) {
            publishDirect(ver);
        }
        return ver;
    }

    @Transactional
    public void submitForApproval(Long versionId) {
        var ver = versionMapper.selectById(versionId);
        if (ver == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"DRAFT".equals(ver.getStatus()))
            throw new BusinessException(ErrorCode.CONFLICT, "Only DRAFT versions can be submitted");

        var param = paramMapper.selectById(ver.getParamId());
        if (param == null || !Boolean.TRUE.equals(param.getApprovalRequired())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Only approval-required parameters use the approval flow");
        }
        ver.setStatus("PENDING_APPROVAL");
        ver.setUpdatedBy(actor());
        versionMapper.updateById(ver);
        approvalRepo.submit("SYSTEM_PARAMETER", String.valueOf(ver.getId()),
                param.getParamName() + " v" + ver.getVersion());
    }

    @Transactional
    public void approveVersion(Long versionId) {
        var ver = versionMapper.selectById(versionId);
        if (ver == null || !"PENDING_APPROVAL".equals(ver.getStatus()))
            throw new BusinessException(ErrorCode.CONFLICT, "Version is not pending approval");

        retireActiveVersion(ver.getParamId());
        ver.setStatus("ACTIVE");
        ver.setEffectiveFrom(LocalDateTime.now());
        ver.setPublishedBy(actor());
        ver.setPublishedAt(LocalDateTime.now());
        ver.setUpdatedBy(actor());
        versionMapper.updateById(ver);
    }

    @Transactional
    public void rejectVersion(Long versionId) {
        var ver = versionMapper.selectById(versionId);
        if (ver == null || !"PENDING_APPROVAL".equals(ver.getStatus()))
            throw new BusinessException(ErrorCode.CONFLICT, "Version is not pending approval");
        ver.setStatus("REJECTED");
        ver.setUpdatedBy(actor());
        versionMapper.updateById(ver);
    }

    @Transactional
    public void withdrawVersion(Long versionId) {
        var ver = versionMapper.selectById(versionId);
        if (ver == null || !"PENDING_APPROVAL".equals(ver.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Version is not pending approval");
        }
        ver.setStatus("DRAFT");
        ver.setUpdatedBy(actor());
        versionMapper.updateById(ver);
    }

    @Transactional
    public SysParameterVersionEntity createVersionWithApproval(Long paramId, String paramValue) {
        return createVersion(paramId, paramValue);
    }

    public boolean isApprovalRequired(Long paramId) {
        var p = paramMapper.selectById(paramId);
        return p != null && Boolean.TRUE.equals(p.getApprovalRequired());
    }

    public String getParamName(Long paramId) {
        var p = paramMapper.selectById(paramId);
        return p != null ? p.getParamName() : null;
    }

    public String getEffectiveValue(String paramCode) {
        var param = paramMapper.selectOne(new LambdaQueryWrapper<SysParameterEntity>()
                .eq(SysParameterEntity::getParamCode, paramCode));
        if (param == null) return null;
        var activeVer = getActiveVersion(param.getId());
        return activeVer != null ? activeVer.getParamValue() : param.getDefaultValue();
    }

    private void publishDirect(SysParameterVersionEntity ver) {
        retireActiveVersion(ver.getParamId());
        ver.setStatus("ACTIVE");
        ver.setEffectiveFrom(LocalDateTime.now());
        ver.setPublishedBy(actor());
        ver.setPublishedAt(LocalDateTime.now());
        ver.setUpdatedBy(actor());
        versionMapper.updateById(ver);
    }

    private void retireActiveVersion(Long paramId) {
        var active = getActiveVersion(paramId);
        if (active != null) {
            active.setStatus("RETIRED"); active.setUpdatedBy(actor());
            versionMapper.updateById(active);
        }
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
