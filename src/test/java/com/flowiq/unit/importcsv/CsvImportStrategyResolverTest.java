package com.flowiq.unit.importcsv;

import com.flowiq.importcsv.CsvImportStrategy;
import com.flowiq.importcsv.CsvImportStrategyResolver;
import com.flowiq.importcsv.CsvParseException;
import com.flowiq.importcsv.MonobankCsvStrategy;
import com.flowiq.importcsv.PrivatBankCsvStrategy;
import com.flowiq.importcsv.UniversalCsvStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CsvImportStrategyResolver tests")
class CsvImportStrategyResolverTest {

    private CsvImportStrategyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CsvImportStrategyResolver(
                new MonobankCsvStrategy(),
                new PrivatBankCsvStrategy(),
                new UniversalCsvStrategy()
        );
    }

    @Test
    @DisplayName("resolves Monobank strategy from headers")
    void resolve_monobank() {
        String csv = "Дата,Опис,MCC,Сума в гривнях\n01.06.2026,Test,5411,-100";

        CsvImportStrategy strategy = resolver.resolve(csv);

        assertThat(strategy.getBankName()).isEqualTo("Monobank");
    }

    @Test
    @DisplayName("resolves PrivatBank strategy from headers")
    void resolve_privatbank() {
        String csv = "Дата,Картка,Опис,Сума в гривнях\n01.06.2026,1234,Test,-100";

        CsvImportStrategy strategy = resolver.resolve(csv);

        assertThat(strategy.getBankName()).isEqualTo("PrivatBank");
    }

    @Test
    @DisplayName("resolves Universal strategy from headers")
    void resolve_universal() {
        String csv = "date,type,category,amount\n2026-06-01,EXPENSE,Office,100";

        CsvImportStrategy strategy = resolver.resolve(csv);

        assertThat(strategy.getBankName()).isEqualTo("Universal");
    }

    @Test
    @DisplayName("throws for empty CSV")
    void resolve_emptyCsv() {
        assertThatThrownBy(() -> resolver.resolve(""))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("throws for unsupported format")
    void resolve_unsupported() {
        assertThatThrownBy(() -> resolver.resolve("foo,bar\n1,2"))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Unsupported CSV format");
    }
}
