package com.flowiq.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaPatcher {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void patchSchema() {
        addColumnIfMissing("transactions", "auto_categorized",
                "ALTER TABLE transactions ADD COLUMN auto_categorized BOOLEAN NOT NULL DEFAULT false");
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        Boolean exists = jdbcTemplate.query(
                """
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = ? AND column_name = ?
                )
                """,
                rs -> {
                    rs.next();
                    return rs.getBoolean(1);
                },
                table,
                column
        );

        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        log.info("Patching schema: adding {}.{}", table, column);
        jdbcTemplate.execute(ddl);
    }
}
