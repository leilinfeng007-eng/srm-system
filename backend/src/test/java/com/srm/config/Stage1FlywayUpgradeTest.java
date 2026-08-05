package com.srm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class Stage1FlywayUpgradeTest {
    private static final String URL = "jdbc:h2:mem:srm_stage1_upgrade;MODE=MySQL;"
            + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    @Test
    void v1ToV3DatabaseUpgradesToLatestWithoutLosingExistingDataAndRestartIsClean()
            throws Exception {
        Flyway legacy = Flyway.configure().dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("3"))
                .load();
        assertThat(legacy.migrate().migrationsExecuted).isEqualTo(3);

        try (var connection = DriverManager.getConnection(URL, "sa", "");
             var statement = connection.prepareStatement("""
                     INSERT INTO sys_operation_log
                       (action_code, target_type, target_id, result_code, detail_summary, trace_id)
                     VALUES ('UPGRADE_MARKER', 'TEST', 'V3', 'SUCCESS', 'preserve-me', 'upgrade-test')
                     """)) {
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }

        Flyway latest = Flyway.configure().dataSource(URL, "sa", "")
                .locations("classpath:db/migration").load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(10);
        assertThat(latest.info().pending()).isEmpty();
        assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("13");

        try (var connection = DriverManager.getConnection(URL, "sa", "");
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sys_operation_log WHERE action_code='UPGRADE_MARKER'");
             var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }

        assertThat(latest.migrate().migrationsExecuted).isZero();
        assertThat(latest.info().pending()).isEmpty();
    }
}
