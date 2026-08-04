package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.system.infrastructure.persistence.MybatisAttachmentRepository;
import com.srm.system.infrastructure.persistence.MybatisBatchJobRepository;
import com.srm.system.infrastructure.persistence.MybatisDictionaryRepository;
import com.srm.system.infrastructure.persistence.MybatisNumberRuleRepository;
import com.srm.system.infrastructure.persistence.MybatisParameterRepository;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.application.service.BatchJobApplicationService;
import com.srm.system.application.service.AuthorizationCatalogApplicationService;
import com.srm.system.api.response.MenuCatalogNode;
import com.srm.system.domain.model.BatchJob;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1GovernanceIntegrationTest {

    @Autowired private MybatisAttachmentRepository attachRepo;
    @Autowired private MybatisBatchJobRepository batchRepo;
    @Autowired private MybatisDictionaryRepository dictRepo;
    @Autowired private MybatisParameterRepository paramRepo;
    @Autowired private MybatisNumberRuleRepository numberRepo;
    @Autowired private SystemGovernanceFacade governance;
    @Autowired private BatchJobApplicationService batchService;
    @Autowired private AuthorizationCatalogApplicationService authorizationCatalog;

    private static Long dictId;
    private static Long paramId;

    @BeforeEach
    void setupAuth() {
        var principal = new SrmPrincipal(1L, "stage0_admin", "", "Stage0 Admin", "ACTIVE",
                false, List.of("SUPER_ADMIN"), List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test @Order(1)
    void dictionaryCrudAndUniqueConstraint() {
        var d = dictRepo.createDict("TEST_STATUS", "Test Status", "Test dict");
        dictId = d.getId();
        assertThat(d.getId()).isNotNull();

        assertThatThrownBy(() -> dictRepo.createDict("TEST_STATUS", "Dup", null))
                .hasMessageContaining("already exists");

        var item = dictRepo.createItem(dictId, "ACT", "Active", 1);
        assertThat(item.getDictId()).isEqualTo(dictId);
        assertThatThrownBy(() -> dictRepo.createItem(dictId, "ACT", "Dup", 2))
                .hasMessageContaining("already exists");

        dictRepo.toggleItem(item.getId(), "INACTIVE");
        dictRepo.toggleDict(dictId, "INACTIVE");
    }

    @Test @Order(2)
    void parameterCreateAndVersionLifecycle() {
        var p = paramRepo.createParam("TEST_THRESHOLD", "Threshold", "DECIMAL",
                "0.5", "^[0-9]+(\\.[0-9]+)?$", true, "Test param");
        paramId = p.getId();
        assertThat(p.getApprovalRequired()).isTrue();

        var v1 = paramRepo.createVersion(paramId, "0.8");
        assertThat(v1.getStatus()).isEqualTo("DRAFT");

        assertThat(paramRepo.getActiveVersion(paramId)).isNull();
        assertThat(paramRepo.getEffectiveValue("TEST_THRESHOLD")).isEqualTo("0.5");

        var v2 = paramRepo.createVersion(paramId, "1.0");
        assertThat(v2.getStatus()).isEqualTo("DRAFT");
        assertThat(paramRepo.listVersions(paramId)).extracting(SysParameterVersionEntity::getVersion)
                .containsExactly(2, 1);
    }

    @Test @Order(3)
    void numberRuleConcurrentGeneration() {
        numberRepo.createRule("TEST_ORDER", "Test Order", "ORDER",
                "ORD", "yyyyMMdd", 4, "DAY", false);
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> requests = IntStream.range(0, 32)
                    .mapToObj(ignored -> (Callable<String>) () -> numberRepo.generateNumber("TEST_ORDER"))
                    .toList();
            var futures = executor.invokeAll(requests);
            var generated = new HashSet<String>();
            for (var future : futures) generated.add(future.get(10, TimeUnit.SECONDS));

            assertThat(generated).hasSize(32).allMatch(value -> value.startsWith("ORD"));
            var serials = generated.stream()
                    .map(value -> Integer.parseInt(value.substring(value.length() - 4)))
                    .sorted().toList();
            assertThat(serials).containsExactlyElementsOf(IntStream.rangeClosed(1, 32).boxed().toList());
        } catch (Exception e) {
            throw new AssertionError("Concurrent number generation failed", e);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test @Order(4)
    void parameterEffectiveValueResolution() {
        String val = paramRepo.getEffectiveValue("TEST_THRESHOLD");
        assertThat(val).isEqualTo("0.5");
    }

    @Test @Order(5)
    void attachmentUploadDownloadAndMimeValidation() {
        assertThatThrownBy(() -> attachRepo.upload("test.exe", "application/x-msdownload", new byte[100], "TEST", "1"))
                .hasMessageContaining("not allowed");

        byte[] pdf = "%PDF-1.4\n1 0 obj\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var att = attachRepo.upload("report.pdf", "application/pdf", pdf, "TEST_OWNER", "100");
        assertThat(att.getFileSha256()).isNotNull();
        assertThat(att.getFileSha256()).hasSize(64);

        byte[] data = attachRepo.download(att.getId());
        assertThat(data).containsExactly(pdf);

        assertThatThrownBy(() -> attachRepo.upload("fake.pdf", "application/pdf",
                "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8), "TEST_OWNER", "101"))
                .hasMessageContaining("does not match");

        assertThatThrownBy(() -> attachRepo.upload("report.pdf.exe", "application/pdf", pdf,
                "TEST_OWNER", "102")).hasMessageContaining("extension");

        var list = attachRepo.listByOwner("TEST_OWNER", "100");
        assertThat(list).isNotEmpty();

        attachRepo.deleteAttachment(att.getId());
    }

    @Test @Order(6)
    void batchImportExportIdempotencyRowErrorsAndRealRetry() {
        String suffix = String.valueOf(System.nanoTime());
        String csv = "unitCode,unitName\nBATCH_" + suffix + ",Batch Unit\nBROKEN_" + suffix + ",\n";
        BatchJob submitted = batchService.startImport("UNIT", "import-" + suffix,
                "units.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        BatchJob imported = awaitTerminal(submitted.id(), null);
        assertThat(imported.status()).isEqualTo("PARTIAL_FAILED");
        assertThat(imported.totalCount()).isEqualTo(2);
        assertThat(imported.successCount()).isEqualTo(1);
        assertThat(imported.failCount()).isEqualTo(1);
        assertThat(imported.resultAttachmentId()).isNotNull();
        assertThat(batchService.errors(imported.id())).singleElement()
                .satisfies(error -> assertThat(error.rowNumber()).isEqualTo(3));

        BatchJob duplicate = batchService.startImport("UNIT", "import-" + suffix,
                "ignored.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(duplicate.id()).isEqualTo(imported.id());

        BatchJob export = awaitTerminal(batchService.startExport("UNIT", "export-" + suffix).id(), null);
        assertThat(export.status()).isEqualTo("SUCCEEDED");
        assertThat(export.resultAttachmentId()).isNotNull();
        assertThat(new String(attachRepo.download(export.resultAttachmentId()),
                java.nio.charset.StandardCharsets.UTF_8)).contains("unitCode,unitName", "BATCH_" + suffix);

        String invalid = "wrongHeader\nvalue\n";
        BatchJob failed = awaitTerminal(batchService.startImport("UNIT", "retry-" + suffix,
                "invalid.csv", "text/csv", invalid.getBytes(java.nio.charset.StandardCharsets.UTF_8)).id(), null);
        assertThat(failed.status()).isEqualTo("FAILED");
        var previousCompletion = failed.completedAt();
        batchService.retry(failed.id());
        BatchJob retried = awaitTerminal(failed.id(), previousCompletion);
        assertThat(retried.status()).isEqualTo("FAILED");
        assertThat(retried.completedAt()).isAfter(previousCompletion);
    }

    private BatchJob awaitTerminal(Long jobId, java.time.LocalDateTime afterCompletion) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        BatchJob current;
        do {
            current = batchService.get(jobId);
            boolean terminal = List.of("SUCCEEDED", "PARTIAL_FAILED", "FAILED").contains(current.status());
            boolean newer = afterCompletion == null || (current.completedAt() != null
                    && current.completedAt().isAfter(afterCompletion));
            if (terminal && newer) return current;
            try { Thread.sleep(25); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for batch job", interrupted);
            }
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Batch job did not reach terminal state: " + current);
    }

    @Test @Order(7)
    void documentTemplateVersionPublishAndImmutability() {
        String code = "TEST_TEMPLATE_" + System.nanoTime();
        var first = (SysDocumentTemplateEntity) governance.createDocumentTemplate(
                code, "Test Template", "Import", "masterdata");
        byte[] pdf = "%PDF-1.4\n1 0 obj\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var firstAttachment = attachRepo.upload("template-v1.pdf", "application/pdf", pdf,
                "DOCUMENT_TEMPLATE", String.valueOf(first.getId()));
        governance.bindDocumentTemplateAttachment(first.getId(), firstAttachment.getId());
        governance.publishDocumentTemplate(first.getId());
        assertThat(((SysDocumentTemplateEntity) governance.documentTemplate(first.getId())).getStatus())
                .isEqualTo("PUBLISHED");
        assertThatThrownBy(() -> governance.bindDocumentTemplateAttachment(
                first.getId(), firstAttachment.getId())).hasMessageContaining("draft");

        var second = (SysDocumentTemplateEntity) governance.createDocumentTemplate(
                code, "Test Template", "Import v2", "masterdata");
        assertThat(second.getTemplateVersion()).isEqualTo(2);
        var secondAttachment = attachRepo.upload("template-v2.pdf", "application/pdf", pdf,
                "DOCUMENT_TEMPLATE", String.valueOf(second.getId()));
        governance.bindDocumentTemplateAttachment(second.getId(), secondAttachment.getId());
        governance.publishDocumentTemplate(second.getId());

        assertThat(((SysDocumentTemplateEntity) governance.documentTemplate(first.getId())).getStatus())
                .isEqualTo("INACTIVE");
        assertThat(((SysDocumentTemplateEntity) governance.documentTemplate(second.getId())).getStatus())
                .isEqualTo("PUBLISHED");
        assertThat(attachRepo.download(governance.documentTemplateAttachmentId(second.getId())))
                .containsExactly(pdf);
    }

    @Test @Order(8)
    void authorizationCatalogReturnsDatabaseIdsAndEffectiveGrantPaths() {
        var permissions = authorizationCatalog.permissions(1, 1000);
        assertThat(permissions.items()).isNotEmpty()
                .allSatisfy(permission -> assertThat(permission.id()).isNotNull());
        assertThat(permissions.items()).extracting(p -> p.permissionCode())
                .contains("system:role:assign-user", "system:batch-job:import");

        var menus = authorizationCatalog.menus();
        assertThat(menus).isNotEmpty();
        assertThat(flattenMenuIds(menus)).doesNotContainNull().hasSizeGreaterThanOrEqualTo(18);

        var effective = authorizationCatalog.effectivePermissions(1L);
        assertThat(effective.permissions()).isNotEmpty()
                .allSatisfy(permission -> {
                    assertThat(permission.sourceRoles()).isNotEmpty();
                    assertThat(permission.authorizationPaths()).isNotEmpty();
                });
    }

    private List<Long> flattenMenuIds(List<MenuCatalogNode> nodes) {
        return nodes.stream().flatMap(node -> java.util.stream.Stream.concat(
                java.util.stream.Stream.of(node.id()),
                flattenMenuIds(node.children()).stream())).toList();
    }
}
