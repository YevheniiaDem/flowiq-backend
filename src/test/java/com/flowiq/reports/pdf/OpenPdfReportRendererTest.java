package com.flowiq.reports.pdf;

import com.flowiq.entity.ReportJob;
import com.flowiq.reports.ReportData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenPdfReportRendererTest {

    @Test
    void rendersValidPdfWithUnicodeContent() {
        PdfFontProvider fonts = new PdfFontProvider();
        OpenPdfReportRenderer renderer = new OpenPdfReportRenderer(fonts);

        ReportData data = ReportData.builder()
                .reportType(ReportJob.ReportType.PROFIT_AND_LOSS)
                .title("Profit & Loss Report")
                .periodFrom(LocalDate.of(2026, 1, 1))
                .periodTo(LocalDate.of(2026, 1, 31))
                .revenue(new BigDecimal("125000.50"))
                .expenses(new BigDecimal("42000.25"))
                .profit(new BigDecimal("82999.25"))
                .taxBurden(new BigDecimal("5400.00"))
                .revenueCategories(List.of(
                        ReportData.CategoryLine.builder()
                                .category("Послуги / Services")
                                .amount(new BigDecimal("125000.50"))
                                .build()
                ))
                .expenseCategories(List.of(
                        ReportData.CategoryLine.builder()
                                .category("Оренда")
                                .amount(new BigDecimal("15000.00"))
                                .build()
                ))
                .monthlyLines(List.of(
                        ReportData.MonthlyLine.builder()
                                .month("2026-01")
                                .revenue(new BigDecimal("125000.50"))
                                .expenses(new BigDecimal("42000.25"))
                                .profit(new BigDecimal("82999.25"))
                                .build()
                ))
                .build();

        byte[] pdf = renderer.render(data);

        assertTrue(pdf.length > 500);
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
    }
}
