package com.flowiq.integration.migration;

import com.flowiq.integration.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway migration integration tests")
class FlywayMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("flyway_schema_history contains successful migrations")
    void flywayHistory_containsSuccessfulMigrations() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class);

        assertThat(migrationCount).isGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("known tables from migrations exist in public schema")
    void knownTables_exist() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """,
                String.class);

        assertThat(tables)
                .contains(
                        "users",
                        "transactions",
                        "notifications",
                        "tasks",
                        "audit_log",
                        "flyway_schema_history"
                );
    }
}
