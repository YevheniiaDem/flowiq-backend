package com.flowiq.unit.importcsv;

import com.flowiq.entity.Transaction;
import com.flowiq.importcsv.CsvParseException;
import com.flowiq.importcsv.PrivatBankCsvStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PrivatBankCsvStrategy tests")
class PrivatBankCsvStrategyTest {

    private PrivatBankCsvStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PrivatBankCsvStrategy();
    }

    @Test
    @DisplayName("supports PrivatBank headers")
    void supports_privatHeaders() {
        assertThat(strategy.supports(new String[]{"Дата", "Картка", "Опис", "Сума в гривнях"})).isTrue();
    }

    @Test
    @DisplayName("returns PrivatBank as bank name")
    void getBankName() {
        assertThat(strategy.getBankName()).isEqualTo("PrivatBank");
    }

    @Test
    @DisplayName("parses PrivatBank CSV and maps categories")
    void parse_validCsv() {
        String csv = """
                Дата,Категорія,Опис,Сума в гривнях
                01.06.2026,services,Consulting fee,3000
                02.06.2026,office,Supplies,-150
                """;

        var rows = strategy.parse(csv);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getType()).isEqualTo(Transaction.Type.REVENUE);
        assertThat(rows.get(0).getCategory()).isEqualTo("Services");
        assertThat(rows.get(1).getType()).isEqualTo(Transaction.Type.EXPENSE);
        assertThat(rows.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("150"));
    }

    @Test
    @DisplayName("maps Ukrainian salary keyword to Salary category")
    void parse_mapsSalaryKeyword() {
        String csv = """
                Дата,Категорія,Опис,Сума в гривнях
                01.06.2026,зарплата,Monthly pay,-5000
                """;

        var rows = strategy.parse(csv);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCategory()).isEqualTo("Salary");
    }

    @Test
    @DisplayName("throws when required columns are missing")
    void parse_missingColumns() {
        assertThatThrownBy(() -> strategy.parse("Опис\nTest"))
                .isInstanceOf(CsvParseException.class);
    }
}
