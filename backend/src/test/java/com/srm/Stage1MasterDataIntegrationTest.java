package com.srm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.masterdata.infrastructure.persistence.entity.*;
import com.srm.masterdata.infrastructure.persistence.mapper.*;
import com.srm.system.api.request.InboxEventRequest;
import com.srm.system.api.request.OutboxEventRequest;
import com.srm.system.application.service.IntegrationApplicationService;
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
import com.srm.security.auth.SrmPrincipal;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1MasterDataIntegrationTest {

    @Autowired private MdPurchasingOrganizationMapper poMapper;
    @Autowired private MdDeliveryLocationMapper dlMapper;
    @Autowired private MdMaterialMapper matMapper;
    @Autowired private MdUnitMapper unitMapper;
    @Autowired private MdCurrencyMapper currMapper;
    @Autowired private MdTaxCodeMapper taxMapper;
    @Autowired private MdCategoryMapper catMapper;
    @Autowired private MybatisIntegrationRepository integrationRepo;
    @Autowired private IntegrationApplicationService integrationService;

    @BeforeEach void setupAuth() { var principal=new SrmPrincipal(1L,"stage0_admin","","Stage0 Admin","ACTIVE",false,List.of("SUPER_ADMIN"),List.of(),null);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities())); }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test @Order(1)
    void purchasingOrganizationCrud() {
        var e = new MdPurchasingOrganizationEntity(); e.setPoCode("PO-TEST-001"); e.setPoName("Test PO"); e.setCompanyOrgId(null); e.setDefaultCurrency("CNY"); e.setStatus("ACTIVE"); e.setSourceType("MANUAL"); e.setSourceSystem("SRM"); e.setCreatedBy("test"); e.setUpdatedBy("test"); poMapper.insert(e);
        assertThat(e.getId()).isNotNull();
        assertThatThrownBy(() -> { var dup = new MdPurchasingOrganizationEntity(); dup.setPoCode("PO-TEST-001"); dup.setPoName("Dup"); dup.setStatus("ACTIVE"); dup.setSourceType("MANUAL"); dup.setSourceSystem("SRM"); dup.setCreatedBy("t"); dup.setUpdatedBy("t"); poMapper.insert(dup); }).isInstanceOf(Exception.class);

        e.setStatus("INACTIVE"); poMapper.updateById(e);
        var loaded = poMapper.selectById(e.getId());
        assertThat(loaded.getStatus()).isEqualTo("INACTIVE");
    }

    @Test @Order(2)
    void materialCrudAndSearch() {
        var e = new MdMaterialEntity(); e.setMaterialCode("MAT-TEST-001"); e.setMaterialName("Test Material"); e.setSpecification("Spec A"); e.setMaterialType("RAW"); e.setBaseUnit("KG"); e.setStatus("ACTIVE"); e.setSourceType("MANUAL"); e.setSourceSystem("SRM"); e.setMaterialVersion("1"); e.setIsCritical(true); e.setCreatedBy("t"); e.setUpdatedBy("t"); matMapper.insert(e);
        assertThat(e.getId()).isNotNull();
        assertThat(e.getIsCritical()).isTrue();

        e.setMaterialName("Updated Material"); matMapper.updateById(e);
        var loaded = matMapper.selectById(e.getId());
        assertThat(loaded.getMaterialName()).isEqualTo("Updated Material");

        e.setStatus("INACTIVE"); matMapper.updateById(e);
        assertThat(matMapper.selectById(e.getId()).getStatus()).isEqualTo("INACTIVE");
    }

    @Test @Order(3)
    void unitAndCurrencyCrud() {
        var u = new MdUnitEntity(); u.setUnitCode("UT-TEST"); u.setUnitName("Test Unit"); u.setStatus("ACTIVE"); u.setSourceType("MANUAL"); u.setSourceSystem("SRM"); u.setCreatedBy("t"); u.setUpdatedBy("t"); unitMapper.insert(u);
        assertThat(u.getId()).isNotNull();

        var c = new MdCurrencyEntity(); c.setCurrencyCode("TST"); c.setCurrencyName("Test Currency"); c.setSymbol("T"); c.setDecimalPlaces(2); c.setStatus("ACTIVE"); c.setSourceType("MANUAL"); c.setSourceSystem("SRM"); c.setCreatedBy("t"); c.setUpdatedBy("t"); currMapper.insert(c);
        assertThat(c.getSymbol()).isEqualTo("T");
    }

    @Test @Order(4)
    void taxCodeCrud() {
        var e = new MdTaxCodeEntity(); e.setTaxCode("VAT-TEST"); e.setTaxName("Test VAT"); e.setCountry("CN"); e.setTaxRate(new java.math.BigDecimal("13.00")); e.setStatus("ACTIVE"); e.setSourceType("MANUAL"); e.setSourceSystem("SRM"); e.setCreatedBy("t"); e.setUpdatedBy("t"); taxMapper.insert(e);
        assertThat(e.getTaxRate().doubleValue()).isEqualTo(13.0);
    }

    @Test @Order(5)
    void categoryCrud() {
        var e = new MdCategoryEntity(); e.setCategoryCode("CAT-TEST"); e.setCategoryName("Test Cat"); e.setLevel(0); e.setRiskLevel("LOW"); e.setStatus("ACTIVE"); e.setSourceType("MANUAL"); e.setSourceSystem("SRM"); e.setCreatedBy("t"); e.setUpdatedBy("t"); catMapper.insert(e);
        assertThat(e.getRiskLevel()).isEqualTo("LOW");
    }

    @Test @Order(6)
    void deliveryLocationCrud() {
        var e = new MdDeliveryLocationEntity(); e.setLocationCode("DL-TEST"); e.setLocationName("Test Location"); e.setAddress("123 Test St"); e.setStatus("ACTIVE"); e.setSourceType("MANUAL"); e.setSourceSystem("SRM"); e.setCreatedBy("t"); e.setUpdatedBy("t"); dlMapper.insert(e);
        assertThat(e.getAddress()).isEqualTo("123 Test St");
    }

    @Test @Order(7)
    void inboxIdempotencyAndVersionGuard() {
        var request = new InboxEventRequest("INBOX-EVT-001", "ERP", "MATERIAL", "100", 1L,
                "{\"materialCode\":\"SYNC-MAT-100\",\"materialName\":\"Synced material\","
                        + "\"materialType\":\"RAW\",\"baseUnit\":\"EA\"}");
        var e1 = integrationService.receiveInbox(request);
        assertThat(e1.status()).isEqualTo("PROCESSED");
        var e2 = integrationService.receiveInbox(request);
        assertThat(e2.id()).isEqualTo(e1.id());
        assertThat(matMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdMaterialEntity>()
                .eq(MdMaterialEntity::getSourceSystem, "ERP")
                .eq(MdMaterialEntity::getExternalId, "100"))).isEqualTo(1);
    }

    @Test @Order(8)
    void outboxCreateAndFailureRetry() {
        var e = integrationService.createOutbox(new OutboxEventRequest("OUTBOX-EVT-001",
                "MATERIAL", "200", 1L, "CREATED", "Summary", "trace-123"));
        assertThat(e.status()).isEqualTo("READY");
        assertThat(integrationRepo.claimOutbox(e.id(), LocalDateTime.now())).isTrue();
        integrationRepo.markOutboxFailed(e.id(), "Network timeout", LocalDateTime.now().plusMinutes(2));
        var failed = integrationRepo.listOutbox(1, 10, "FAILED").items();
        assertThat(failed).isNotEmpty();
        assertThat(failed.get(0).attemptCount()).isGreaterThan(0);

        var dup = integrationService.createOutbox(new OutboxEventRequest("OUTBOX-EVT-001",
                "MATERIAL", "200", 1L, "CREATED", "Dup", "trace-456"));
        assertThat(dup.id()).isEqualTo(e.id());
    }
}
