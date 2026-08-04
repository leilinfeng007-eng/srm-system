package com.srm;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.system.infrastructure.persistence.MybatisAuditRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Stage1AuditTest {

    @Autowired private MybatisAuditRepository auditService;

    @BeforeEach
    void auth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("audit_admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void auditRecordsOperatorBeforeAfterAndMaskedSensitive() {
        auditService.record("PARAM_VERSION_APPROVED", "PARAM_VERSION", "999",
                "SUCCESS", "paramValue=0.8", "paramValue=1.0", "approval approved");

        var logs = auditService.findByTarget("PARAM_VERSION", "999");
        assertThat(logs).isNotEmpty();
        var log = logs.get(0);
        assertThat(log.getOperatorName()).isEqualTo("audit_admin");
        assertThat(log.getActionCode()).isEqualTo("PARAM_VERSION_APPROVED");
        assertThat(log.getBeforeHash()).isNotEqualTo(log.getAfterHash());
        assertThat(log.getFieldChanges()).contains("0.8").contains("1.0");
        assertThat(log.getFieldChanges()).doesNotContain("***MASKED***");
    }

    @Test
    void sensitiveValuesAreMaskedInAudit() {
        auditService.record("RESET_PASSWORD", "USER", "7",
                "SUCCESS", "password=S3cr3t!Pw", "password=TempPass!1", "admin reset");

        var logs = auditService.findByTarget("USER", "7");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getFieldChanges()).contains("***MASKED***");
        assertThat(logs.get(0).getFieldChanges()).doesNotContain("S3cr3t!Pw");
    }

    @Test
    void roleDataPolicyChangeAudited() {
        auditService.recordRoleDataPolicyChange(5L, "masterdata:CATEGORY=ORG;", "masterdata:CATEGORY=ALL;", "scope widened");

        var logs = auditService.findByTarget("ROLE", "5");
        assertThat(logs).isNotEmpty();
        var log = logs.get(0);
        assertThat(log.getActionCode()).isEqualTo("ROLE_DATA_POLICY_UPDATED");
        assertThat(log.getBeforeHash()).isNotEqualTo(log.getAfterHash());
        assertThat(log.getFieldChanges()).contains("ORG").contains("ALL");
    }

}
