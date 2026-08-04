package com.srm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {

    private static final List<String> DOMAIN_PACKAGES = List.of(
            "workbench",
            "supplier",
            "sourcing",
            "contract",
            "source",
            "procurement",
            "delivery",
            "quality",
            "settlement",
            "performance",
            "masterdata",
            "system");

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.srm");
    }

    @Test
    void sharedFoundationMustNotDependOnBusinessDomains() {
        String[] domainPatterns = DOMAIN_PACKAGES.stream()
                .map(name -> "com.srm." + name + "..")
                .toArray(String[]::new);
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "com.srm.common..",
                        "com.srm.config..",
                        "com.srm.security..",
                        "com.srm.platform..")
                .should().dependOnClassesThat().resideInAnyPackage(domainPatterns);
        rule.check(classes);
    }

    @Test
    void domainsMustNotReachIntoAnotherDomainsInfrastructure() {
        for (String source : DOMAIN_PACKAGES) {
            for (String target : DOMAIN_PACKAGES) {
                if (!source.equals(target)) {
                    noClasses()
                            .that().resideInAPackage("com.srm." + source + "..")
                            .should().dependOnClassesThat()
                            .resideInAPackage("com.srm." + target + ".infrastructure..")
                            .check(classes);
                }
            }
        }
    }

    @Test
    void apiLayerMustNotDependOnInfrastructureDetails() {
        noClasses()
                .that().resideInAPackage("com.srm..api..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.srm..infrastructure..", "com.baomidou.mybatisplus..")
                .check(classes);
    }

    @Test
    void workbenchApiAndDomainMustRespectTheReferenceLayering() {
        noClasses()
                .that().resideInAPackage("com.srm.workbench.api..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.srm.workbench.infrastructure..")
                .check(classes);
        noClasses()
                .that().resideInAPackage("com.srm.workbench.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.srm.workbench.api..",
                        "com.srm.workbench.application..",
                        "com.srm.workbench.infrastructure..")
                .check(classes);
    }

    @Test
    void applicationAndDomainLayersMustNotDependOnPersistenceDetails() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.srm..application..",
                        "com.srm..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.srm..infrastructure..", "com.baomidou.mybatisplus..")
                .check(classes);
    }

    @Test
    void directSpringJdbcUsageMustRemainEliminated() {
        noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.jdbc.core..")
                .check(classes);
    }
}
