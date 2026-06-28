package com.flowiq.importcsv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CsvLineParser tests")
class CsvLineParserTest {

    @Test
    @DisplayName("parseLine handles quoted commas and escaped quotes")
    void parseLine_quotedFields() {
        String[] cells = CsvLineParser.parseLine("a,\"b,c\",\"d\"\"e\"");

        assertThat(cells).containsExactly("a", "b,c", "d\"e");
    }

    @Test
    @DisplayName("parseLines skips blank lines")
    void parseLines_skipsBlankLines() {
        var lines = CsvLineParser.parseLines("header\n\nvalue");

        assertThat(lines).hasSize(2);
    }

    @Test
    @DisplayName("findColumn matches partial header names")
    void findColumn_partialMatch() {
        String[] headers = CsvLineParser.normalizeHeaders(new String[]{"Date (UTC)", "Amount (UAH)"});

        assertThat(CsvLineParser.findColumn(headers, "date")).isZero();
        assertThat(CsvLineParser.findColumn(headers, "amount (uah)")).isEqualTo(1);
        assertThat(CsvLineParser.findColumn(headers, "missing")).isEqualTo(-1);
    }

    @Test
    @DisplayName("parseDate supports ISO and dd.MM.yyyy formats")
    void parseDate_multipleFormats() {
        assertThat(CsvLineParser.parseDate("2026-06-01")).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(CsvLineParser.parseDate("01.06.2026")).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(CsvLineParser.parseDate("01.06.2026 14:30:00")).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    @DisplayName("parseDate rejects blank values")
    void parseDate_blank() {
        assertThatThrownBy(() -> CsvLineParser.parseDate("  "))
                .isInstanceOf(CsvParseException.class);
    }

    @Test
    @DisplayName("parseAmount normalizes spaces and comma decimals")
    void parseAmount_normalized() {
        assertThat(CsvLineParser.parseAmount("1 500,50")).isEqualByComparingTo(new BigDecimal("1500.50"));
    }

    @Test
    @DisplayName("parseAmount rejects invalid numbers")
    void parseAmount_invalid() {
        assertThatThrownBy(() -> CsvLineParser.parseAmount("not-a-number"))
                .isInstanceOf(CsvParseException.class);
    }

    @Test
    @DisplayName("cell returns empty string for out-of-range index")
    void cell_outOfRange() {
        assertThat(CsvLineParser.cell(new String[]{"a"}, 5)).isEmpty();
    }
}
