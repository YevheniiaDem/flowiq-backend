package com.flowiq.unit.importcsv;

import com.flowiq.entity.Transaction;
import com.flowiq.importcsv.CsvParseException;
import com.flowiq.importcsv.MonobankCsvStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MonobankCsvStrategy tests")
class MonobankCsvStrategyTest {

    private MonobankCsvStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MonobankCsvStrategy();
    }

    @Test
    @DisplayName("supports Monobank headers with MCC column")
    void supports_monobankHeaders() {
        assertThat(strategy.supports(new String[]{"Дата", "Опис", "MCC", "Сума в гривнях"})).isTrue();
    }

    @Test
    @DisplayName("returns Monobank as bank name")
    void getBankName() {
        assertThat(strategy.getBankName()).isEqualTo("Monobank");
    }

    @Test
    @DisplayName("parses Monobank CSV with positive and negative amounts")
    void parse_validCsv() {
        String csv = """
                Дата,Опис,MCC,Сума в гривнях
                01.06.2026,Client payment,,1500.00
                02.06.2026,Office supplies,5411,-200.50
                """;

        var rows = strategy.parse(csv);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getType()).isEqualTo(Transaction.Type.REVENUE);
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(rows.get(1).getType()).isEqualTo(Transaction.Type.EXPENSE);
        assertThat(rows.get(1).getCategory()).isEqualTo("Office");
    }

    @Test
    @DisplayName("skips zero-amount rows")
    void parse_skipsZeroAmount() {
        String csv = """
                Дата,Сума в гривнях
                01.06.2026,0
                02.06.2026,100
                """;

        assertThat(strategy.parse(csv)).hasSize(1);
    }

    @Test
    @DisplayName("returns empty list for empty CSV")
    void parse_emptyCsv() {
        assertThat(strategy.parse("")).isEmpty();
    }

    @Test
    @DisplayName("throws when required columns are missing")
    void parse_missingColumns() {
        assertThatThrownBy(() -> strategy.parse("Опис,Сума\nTest,100"))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("required columns");
    }
}
