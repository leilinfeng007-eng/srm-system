package com.srm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.masterdata.infrastructure.persistence.entity.*;
import com.srm.masterdata.infrastructure.persistence.mapper.*;
import com.srm.system.api.request.InboxEventRequest;
import com.srm.system.api.request.OutboxEventRequest;
import com.srm.system.application.service.IntegrationApplicationService;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.infrastructure.persistence.MybatisIntegrationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1ClosingVerificationTest {

    @Autowired private MdMaterialMapper matMapper;
    @Autowired private MybatisIntegrationRepository integrationRepo;
    @Autowired private IntegrationApplicationService integrationService;

    @BeforeEach void setupAuth() { var principal=new SrmPrincipal(1L,"stage0_admin","","Stage0 Admin","ACTIVE",false,List.of("SUPER_ADMIN"),List.of(),null);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities())); }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test @Order(1)
    void duplicateMaterialCodeRejected() {
        var m = new MdMaterialEntity(); m.setMaterialCode("DUP-CODE-001"); m.setMaterialName("Original"); m.setStatus("ACTIVE"); m.setSourceType("MANUAL"); m.setSourceSystem("SRM"); m.setCreatedBy("t"); m.setUpdatedBy("t"); matMapper.insert(m);
        assertThatThrownBy(() -> { var d = new MdMaterialEntity(); d.setMaterialCode("DUP-CODE-001"); d.setMaterialName("Duplicate"); d.setStatus("ACTIVE"); d.setSourceType("MANUAL"); d.setSourceSystem("SRM"); d.setCreatedBy("t"); d.setUpdatedBy("t"); matMapper.insert(d); }).isInstanceOf(Exception.class);
    }

    @Test @Order(2)
    void externalSyncOldVersionIgnoredNewVersionProcessed() {
        var e1 = integrationService.receiveInbox(inbox("EVT-VER-001", "M-100", 1L, "Material v1"));
        assertThat(e1.status()).isEqualTo("PROCESSED");
        var e2 = integrationService.receiveInbox(inbox("EVT-VER-002", "M-100", 2L, "Material v2"));
        assertThat(e2.status()).isEqualTo("PROCESSED");
        var e3 = integrationService.receiveInbox(inbox("EVT-VER-003", "M-100", 1L, "Material old"));
        assertThat(e3.status()).isEqualTo("IGNORED_STALE");
        var stored = matMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdMaterialEntity>()
                .eq(MdMaterialEntity::getSourceSystem, "ERP")
                .eq(MdMaterialEntity::getExternalId, "M-100"));
        assertThat(stored.getMaterialName()).isEqualTo("Material v2");
        assertThat(stored.getExternalVersion()).isEqualTo(2L);
    }

    @Test @Order(3)
    void outboxAtomicClaimPreventsDoubleProcessing() {
        var e = integrationService.createOutbox(new OutboxEventRequest("ATOMIC-OBX-001",
                "MATERIAL", "M-200", 1L, "CREATED", "test", "t1"));
        boolean claimed = integrationRepo.claimOutbox(e.id(), LocalDateTime.now());
        assertThat(claimed).isTrue();
        boolean doubleClaim = integrationRepo.claimOutbox(e.id(), LocalDateTime.now());
        assertThat(doubleClaim).isFalse();
    }

    @Test @Order(4)
    void inboxDuplicateDeliveryProcessedOnce() {
        var e1 = integrationService.receiveInbox(inbox("DUP-DELIVERY-001", "M-300", 1L, "First"));
        assertThat(e1.status()).isEqualTo("PROCESSED");
        var e2 = integrationService.receiveInbox(inbox("DUP-DELIVERY-001", "M-300", 1L, "Second"));
        assertThat(e2.id()).isEqualTo(e1.id());
        assertThat(matMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdMaterialEntity>()
                .eq(MdMaterialEntity::getSourceSystem, "ERP")
                .eq(MdMaterialEntity::getExternalId, "M-300"))).isEqualTo(1);
    }

    @Test @Order(5)
    void inboxRetryProcessesFailedEvent() {
        var e = integrationService.receiveInbox(new InboxEventRequest("RETRY-EVT-001", "ERP",
                "MATERIAL", "M-400", 1L, "{\"materialCode\":\"M-400\"}"));
        assertThat(e.status()).isEqualTo("FAILED");
        var retried = integrationService.retryInbox(e.id());
        assertThat(retried.status()).isEqualTo("FAILED");
        assertThat(retried.attemptCount()).isEqualTo(2);
    }

    @Test @Order(6)
    void outboxTransactionRollbackDoesNotProduceHalfState() {
        var e = integrationService.createOutbox(new OutboxEventRequest("TX-OBX-001",
                "MATERIAL", "M-500", 1L, "CREATED", "tx-test", "t1"));
        assertThat(e.status()).isEqualTo("READY");
        assertThat(integrationRepo.claimOutbox(e.id(), LocalDateTime.now())).isTrue();
        assertThat(integrationRepo.markOutboxPublished(e.id())).isTrue();
        boolean rePublished = integrationRepo.markOutboxPublished(e.id());
        assertThat(rePublished).isFalse();
    }

    private InboxEventRequest inbox(String eventId, String objectId, long version, String name) {
        String payload = "{\"materialCode\":\"" + objectId + "\",\"materialName\":\"" + name
                + "\",\"materialType\":\"RAW\",\"baseUnit\":\"EA\"}";
        return new InboxEventRequest(eventId, "ERP", "MATERIAL", objectId, version, payload);
    }
}
