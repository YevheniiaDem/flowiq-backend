package com.flowiq.unit.importcsv;

import com.flowiq.entity.Transaction;
import com.flowiq.importcsv.CsvParseException;
import com.flowiq.importcsv.ParsedTransactionRow;
import com.flowiq.importcsv.UniversalCsvStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UniversalCsvStrategy tests")
class UniversalCsvStrategyTest {

    private UniversalCsvStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new UniversalCsvStrategy();
    }

    @Test
    @DisplayName("supports universal CSV headers")
    void supports_universalHeaders() {
        assertThat(strategy.supports(new String[]{"date", "type", "category", "amount"})).isTrue();
    }

    @Test
    @DisplayName("parses valid universal CSV rows")
    void parse_validCsv() {
        String csv = """
                date,type,category,amount,description
                2026-06-01,EXPENSE,Office,1500.50,Office rent
                2026-06-02,INCOME,Services,3000,Consulting
                """;

        List<ParsedTransactionRow> rows = strategy.parse(csv);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getType()).isEqualTo(Transaction.Type.EXPENSE);
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1500.50"));
        assertThat(rows.get(1).getType()).isEqualTo(Transaction.Type.REVENUE);
    }

    @Test
    @DisplayName("returns empty list for empty CSV content")
    void parse_emptyCsv() {
        assertThat(strategy.parse("")).isEmpty();
    }

    @Test
    @DisplayName("skips rows with invalid category")
    void parse_skipsInvalidCategory() {
        String csv = """
                date,type,category,amount
                2026-06-01,EXPENSE,InvalidCategory,100
                2026-06-02,EXPENSE,Office,200
                """;

        List<ParsedTransactionRow> rows = strategy.parse(csv);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCategory()).isEqualTo("Office");
    }

    @Test
    @DisplayName("throws when required columns are missing")
    void parse_missingColumns() {
        String csv = """
                date,amount
                2026-06-01,100
                """;

        assertThatThrownBy(() -> strategy.parse(csv))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("required columns");
    }

    @Test
    @DisplayName("parses quoted CSV fields with commas")
    void parse_quotedFields() {
        String csv = """
                date,type,category,amount,description
                2026-06-01,EXPENSE,Office,100,"Rent, June"
                """;

        List<ParsedTransactionRow> rows = strategy.parse(csv);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDescription()).isEqualTo("Rent, June");
        assertThat(rows.get(0).getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }
}
