package com.srm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.infrastructure.persistence.entity.*;
import com.srm.masterdata.infrastructure.persistence.mapper.*;
import com.srm.system.infrastructure.persistence.MybatisAttachmentRepository;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.SysAttachmentMapper;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import com.srm.security.auth.SrmPrincipal;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1QualityGateTest {

    @Autowired private MybatisAttachmentRepository attachRepo;
    @Autowired private SysAttachmentMapper attachMapper;
    @Autowired private MdMaterialMapper matMapper;
    @Autowired private MdCategoryMapper catMapper;

    @BeforeEach void setupAuth() { var principal=new SrmPrincipal(1L,"stage0_admin","","Stage0 Admin","ACTIVE",false,List.of("SUPER_ADMIN"),List.of(),null);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities())); }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test @Order(1)
    void attachmentRejectsDisallowedMime() {
        assertThatThrownBy(() -> attachRepo.upload("evil.exe", "application/x-msdownload", new byte[10], "TEST", "1"))
                .hasMessageContaining("not allowed");
    }

    @Test @Order(2)
    void attachmentUploadDownloadRoundTrip() {
        byte[] pdf = "%PDF-1.4\nquality-gate".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var att = attachRepo.upload("safe.pdf", "application/pdf", pdf, "QA_OWNER", "1");
        assertThat(att.getFileSha256()).hasSize(64);
        assertThat(att.getStorageKey()).doesNotContain("..");
        assertThat(att.getStorageKey()).doesNotContain("..");
        byte[] data = attachRepo.download(att.getId());
        assertThat(data).containsExactly(pdf);
        assertThat(attachRepo.getAttachment(att.getId()).getStatus()).isEqualTo("ACTIVE");
        attachRepo.deleteAttachment(att.getId());
        assertThatThrownBy(() -> attachRepo.getAttachment(att.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThat(attachMapper.selectById(att.getId()).getStatus()).isEqualTo("DELETED");
    }

    @Test @Order(3)
    void attachmentReplaceKeepsMetadataConsistency() {
        byte[] v1 = "%PDF-1.4\nversion-one".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] v2 = "%PDF-1.4\nversion-two".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var att = attachRepo.upload("v1.pdf", "application/pdf", v1, "QA_OWNER", "2");
        var replaced = attachRepo.replaceVersion(att.getId(), "v2.pdf", "application/pdf", v2);
        assertThat(replaced.getFileSha256()).isNotEqualTo(att.getFileSha256());
        assertThat(attachRepo.download(att.getId())).containsExactly(v2);
    }

    @Test @Order(4)
    void masterDataInvalidReferenceRejected() {
        var m = new MdMaterialEntity();
        m.setMaterialCode("MAT-INV-REF"); m.setMaterialName("Bad Ref"); m.setStatus("ACTIVE");
        m.setSourceType("MANUAL"); m.setSourceSystem("SRM"); m.setCreatedBy("t"); m.setUpdatedBy("t");
        m.setCategoryId(99999999L);
        assertThatThrownBy(() -> matMapper.insert(m))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test @Order(5)
    void externalSourceFieldsNotOverwrittenByManualUpdate() {
        var m = new MdMaterialEntity();
        m.setMaterialCode("MAT-EXT-001"); m.setMaterialName("External Material");
        m.setSourceType("EXTERNAL"); m.setSourceSystem("ERP"); m.setExternalId("ERP-MAT-1");
        m.setExternalVersion(2L); m.setStatus("ACTIVE"); m.setCreatedBy("sync"); m.setUpdatedBy("sync");
        matMapper.insert(m);

        m.setMaterialName("Manual Edit Attempt");
        matMapper.updateById(m);
        var loaded = matMapper.selectById(m.getId());
        assertThat(loaded.getSourceType()).isEqualTo("EXTERNAL");
        assertThat(loaded.getSourceSystem()).isEqualTo("ERP");
        assertThat(loaded.getExternalId()).isEqualTo("ERP-MAT-1");
    }

    @Test @Order(6)
    void optimisticLockConflictDetected() {
        var m = new MdMaterialEntity();
        m.setMaterialCode("MAT-OPT-001"); m.setMaterialName("Opt Lock"); m.setStatus("ACTIVE");
        m.setSourceType("MANUAL"); m.setSourceSystem("SRM"); m.setCreatedBy("t"); m.setUpdatedBy("t");
        m.setVersion(1L); matMapper.insert(m);

        var firstWriter = matMapper.selectById(m.getId());
        var staleWriter = matMapper.selectById(m.getId());
        firstWriter.setMaterialName("First committed change");
        staleWriter.setMaterialName("Stale overwrite attempt");

        assertThat(matMapper.updateById(firstWriter)).isEqualTo(1);
        assertThat(matMapper.updateById(staleWriter)).as("stale version must be rejected").isZero();
        var persisted = matMapper.selectById(m.getId());
        assertThat(persisted.getMaterialName()).isEqualTo("First committed change");
        assertThat(persisted.getVersion()).isEqualTo(2L);
    }
}
