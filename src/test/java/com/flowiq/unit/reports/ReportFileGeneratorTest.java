package com.flowiq.unit.reports;

import com.flowiq.entity.ReportJob;
import com.flowiq.reports.ReportData;
import com.flowiq.reports.ReportFileGenerator;
import com.flowiq.reports.excel.PoiReportRenderer;
import com.flowiq.reports.pdf.OpenPdfReportRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportFileGenerator tests")
class ReportFileGeneratorTest {

    @Mock private OpenPdfReportRenderer pdfRenderer;
    @Mock private PoiReportRenderer excelRenderer;

    private ReportFileGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ReportFileGenerator(pdfRenderer, excelRenderer);
    }

    @Test
    @DisplayName("generate CSV includes totals and category breakdown")
    void generate_csv() {
        ReportData data = sampleData();

        byte[] bytes = generator.generate(data, ReportJob.Format.CSV);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("Revenue");
        assertThat(csv).contains("Marketing");
        assertThat(csv).contains("FOP Group");
    }

    @Test
    @DisplayName("generate PDF delegates to renderer")
    void generate_pdf() {
        when(pdfRenderer.render(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] bytes = generator.generate(sampleData(), ReportJob.Format.PDF);

        assertThat(bytes).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("generate Excel delegates to renderer")
    void generate_excel() {
        when(excelRenderer.render(any())).thenReturn(new byte[]{4, 5});

        byte[] bytes = generator.generate(sampleData(), ReportJob.Format.EXCEL);

        assertThat(bytes).containsExactly(4, 5);
    }

    @Test
    @DisplayName("resolveFileName and content type for each format")
    void resolveMetadata() {
        ReportData data = sampleData();

        assertThat(generator.resolveFileName(data, ReportJob.Format.CSV)).endsWith(".csv");
        assertThat(generator.resolveFileName(data, ReportJob.Format.PDF)).endsWith(".pdf");
        assertThat(generator.resolveFileName(data, ReportJob.Format.EXCEL)).endsWith(".xlsx");
        assertThat(generator.resolveContentType(ReportJob.Format.PDF)).isEqualTo("application/pdf");
    }

    private ReportData sampleData() {
        return ReportData.builder()
                .title("Monthly Report")
                .reportType(ReportJob.ReportType.PROFIT_AND_LOSS)
                .periodFrom(LocalDate.of(2026, 6, 1))
                .periodTo(LocalDate.of(2026, 6, 30))
                .revenue(new BigDecimal("10000"))
                .expenses(new BigDecimal("4000"))
                .profit(new BigDecimal("6000"))
                .taxBurden(new BigDecimal("600"))
                .revenueCategories(List.of())
                .expenseCategories(List.of(
                        ReportData.CategoryLine.builder().category("Marketing").amount(new BigDecimal("1000")).build()))
                .monthlyLines(List.of(
                        ReportData.MonthlyLine.builder()
                                .month("2026-06")
                                .revenue(new BigDecimal("10000"))
                                .expenses(new BigDecimal("4000"))
                                .profit(new BigDecimal("6000"))
                                .build()))
                .fopGroup("Group 2")
                .incomeLimitUsagePercent(45.5)
                .annualIncome(new BigDecimal("500000"))
                .incomeLimit(new BigDecimal("5328000"))
                .estimatedTax(new BigDecimal("25000"))
                .taxForecast(new BigDecimal("30000"))
                .build();
    }
}
