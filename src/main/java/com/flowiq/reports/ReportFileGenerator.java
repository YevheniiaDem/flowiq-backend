package com.flowiq.reports;

import com.flowiq.entity.ReportJob;
import com.flowiq.exception.BadRequestException;
import com.flowiq.reports.excel.PoiReportRenderer;
import com.flowiq.reports.pdf.OpenPdfReportRenderer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@lombok.RequiredArgsConstructor
public class ReportFileGenerator {

    private final OpenPdfReportRenderer pdfRenderer;
    private final PoiReportRenderer excelRenderer;

    public byte[] generate(ReportData data, ReportJob.Format format) {
        return switch (format) {
            case CSV -> generateCsv(data);
            case PDF -> generatePdf(data);
            case EXCEL -> generateExcel(data);
        };
    }

    public String resolveFileName(ReportData data, ReportJob.Format format) {
        String base = data.getReportType().name().toLowerCase().replace('_', '-')
                + "_" + data.getPeriodFrom() + "_" + data.getPeriodTo();
        return switch (format) {
            case CSV -> base + ".csv";
            case PDF -> base + ".pdf";
            case EXCEL -> base + ".xlsx";
        };
    }

    public String resolveContentType(ReportJob.Format format) {
        return switch (format) {
            case CSV -> "text/csv";
            case PDF -> "application/pdf";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }

    private byte[] generateCsv(ReportData data) {
        List<String[]> rows = buildRows(data);
        StringBuilder csv = new StringBuilder();
        csv.append(data.getTitle()).append("\n");
        csv.append("Period,").append(data.getPeriodFrom()).append(" to ").append(data.getPeriodTo()).append("\n\n");
        for (String[] row : rows) {
            csv.append(escape(row[0])).append(",").append(escape(row[1])).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generatePdf(ReportData data) {
        return pdfRenderer.render(data);
    }

    private byte[] generateExcel(ReportData data) {
        return excelRenderer.render(data);
    }

    private List<String[]> buildRows(ReportData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Revenue", format(data.getRevenue())});
        rows.add(new String[]{"Expenses", format(data.getExpenses())});
        rows.add(new String[]{"Profit", format(data.getProfit())});
        rows.add(new String[]{"Tax Burden", format(data.getTaxBurden())});

        if (!data.getRevenueCategories().isEmpty()) {
            rows.add(new String[]{"--- Revenue by Category ---", ""});
            for (ReportData.CategoryLine line : data.getRevenueCategories()) {
                rows.add(new String[]{line.getCategory(), format(line.getAmount())});
            }
        }

        if (!data.getExpenseCategories().isEmpty()) {
            rows.add(new String[]{"--- Expenses by Category ---", ""});
            for (ReportData.CategoryLine line : data.getExpenseCategories()) {
                rows.add(new String[]{line.getCategory(), format(line.getAmount())});
            }
        }

        if (!data.getMonthlyLines().isEmpty()) {
            rows.add(new String[]{"--- Monthly Breakdown ---", ""});
            for (ReportData.MonthlyLine line : data.getMonthlyLines()) {
                rows.add(new String[]{
                        line.getMonth(),
                        "Rev " + format(line.getRevenue()) + " / Exp " + format(line.getExpenses())
                                + " / Profit " + format(line.getProfit())
                });
            }
        }

        if (data.getFopGroup() != null) {
            rows.add(new String[]{"--- FOP / Tax ---", ""});
            rows.add(new String[]{"FOP Group", data.getFopGroup()});
            rows.add(new String[]{"Income Limit Usage", String.format("%.1f%%", data.getIncomeLimitUsagePercent())});
            rows.add(new String[]{"Annual Income", format(data.getAnnualIncome())});
            rows.add(new String[]{"Income Limit", format(data.getIncomeLimit())});
            rows.add(new String[]{"Estimated Tax", format(data.getEstimatedTax())});
            rows.add(new String[]{"Tax Forecast", format(data.getTaxForecast())});
        }

        return rows;
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
