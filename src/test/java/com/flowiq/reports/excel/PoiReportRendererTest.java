package com.flowiq.reports.excel;

import com.flowiq.entity.ReportJob;
import com.flowiq.reports.ReportData;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiReportRendererTest {

  private final PoiReportRenderer renderer = new PoiReportRenderer();

  @ParameterizedTest
  @EnumSource(ReportJob.ReportType.class)
  void rendersValidWorkbookForAllReportTypes(ReportJob.ReportType type) throws Exception {
    ReportData data = sampleData(type);
    byte[] bytes = renderer.render(data);

    assertTrue(bytes.length > 500);

    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      assertTrue(workbook.getNumberOfSheets() >= 1);
      assertTrue(workbook.getSheetAt(0).getPhysicalNumberOfRows() > 0);
    }
  }

  private ReportData sampleData(ReportJob.ReportType type) {
    List<ReportData.CategoryLine> revenueCategories = List.of(
        ReportData.CategoryLine.builder()
            .category("Послуги")
            .amount(new BigDecimal("100000"))
            .build()
    );
    List<ReportData.CategoryLine> expenseCategories = List.of(
        ReportData.CategoryLine.builder()
            .category("Оренда")
            .amount(new BigDecimal("20000"))
            .build()
    );
    List<ReportData.MonthlyLine> monthlyLines = List.of(
        ReportData.MonthlyLine.builder()
            .month("2026-01")
            .revenue(new BigDecimal("100000"))
            .expenses(new BigDecimal("20000"))
            .profit(new BigDecimal("80000"))
            .build()
    );

    ReportData.ReportDataBuilder builder = ReportData.builder()
        .reportType(type)
        .title(type.name() + " Report")
        .periodFrom(LocalDate.of(2026, 1, 1))
        .periodTo(LocalDate.of(2026, 1, 31))
        .revenue(new BigDecimal("100000"))
        .expenses(new BigDecimal("20000"))
        .profit(new BigDecimal("80000"))
        .taxBurden(new BigDecimal("5000"))
        .revenueCategories(revenueCategories)
        .expenseCategories(expenseCategories)
        .monthlyLines(monthlyLines)
        .fopGroup("FOP III")
        .incomeLimitUsagePercent(42.5)
        .estimatedTax(new BigDecimal("5000"))
        .taxForecast(new BigDecimal("12000"))
        .annualIncome(new BigDecimal("500000"))
        .incomeLimit(new BigDecimal("1200000"));

    return builder.build();
  }
}
