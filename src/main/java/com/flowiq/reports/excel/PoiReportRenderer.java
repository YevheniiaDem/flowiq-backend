package com.flowiq.reports.excel;

import com.flowiq.entity.ReportJob;
import com.flowiq.exception.BadRequestException;
import com.flowiq.reports.ReportData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PoiReportRenderer {

    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public byte[] render(ReportData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelStyles styles = ExcelStyles.create(workbook);

            switch (data.getReportType()) {
                case PROFIT_AND_LOSS -> renderProfitAndLoss(workbook, styles, data);
                case CASH_FLOW -> renderCashFlow(workbook, styles, data);
                case REVENUE_SUMMARY -> renderRevenueSummary(workbook, styles, data);
                case EXPENSE_SUMMARY -> renderExpenseSummary(workbook, styles, data);
                case TAX_SUMMARY -> renderTaxSummary(workbook, styles, data);
                case FOP_SUMMARY -> renderFopSummary(workbook, styles, data);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    private void renderProfitAndLoss(Workbook workbook, ExcelStyles styles, ReportData data) {
        Sheet summary = workbook.createSheet("Summary");
        writeReportHeader(summary, styles, data);
        int row = writeSectionTitle(summary, styles, 4, "Financial Summary");
        row = writeMetricRow(summary, styles, row, "Revenue (UAH)", data.getRevenue());
        row = writeMetricRow(summary, styles, row, "Expenses (UAH)", data.getExpenses());
        row = writeMetricRow(summary, styles, row, "Profit (UAH)", data.getProfit());
        writeMetricRow(summary, styles, row, "Tax Burden (UAH)", data.getTaxBurden());
        autoSize(summary, 2);

        writeCategorySheet(workbook, styles, data, "Revenue", "Revenue by Category",
                categories(data.getRevenueCategories()));
        writeCategorySheet(workbook, styles, data, "Expenses", "Expenses by Category",
                categories(data.getExpenseCategories()));
    }

    private void renderCashFlow(Workbook workbook, ExcelStyles styles, ReportData data) {
        Sheet sheet = workbook.createSheet("Cash Flow");
        writeReportHeader(sheet, styles, data);
        int row = writeSectionTitle(sheet, styles, 4, "Monthly Cash Flow");

        Row header = sheet.createRow(row++);
        writeHeaderCell(header, 0, styles.header, "Month");
        writeHeaderCell(header, 1, styles.header, "Inflows (UAH)");
        writeHeaderCell(header, 2, styles.header, "Outflows (UAH)");
        writeHeaderCell(header, 3, styles.header, "Net Cash Flow (UAH)");
        sheet.createFreezePane(0, row);

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (ReportData.MonthlyLine line : monthly(data.getMonthlyLines())) {
            Row dataRow = sheet.createRow(row++);
            writeTextCell(dataRow, 0, styles.text, line.getMonth());
            writeAmountCell(dataRow, 1, styles.currency, line.getRevenue());
            writeAmountCell(dataRow, 2, styles.currency, line.getExpenses());
            writeAmountCell(dataRow, 3, styles.currency, line.getProfit());
            totalIn = totalIn.add(nullToZero(line.getRevenue()));
            totalOut = totalOut.add(nullToZero(line.getExpenses()));
            totalNet = totalNet.add(nullToZero(line.getProfit()));
        }

        Row totalRow = sheet.createRow(row);
        writeTextCell(totalRow, 0, styles.label, "Total");
        writeAmountCell(totalRow, 1, styles.currency, totalIn);
        writeAmountCell(totalRow, 2, styles.currency, totalOut);
        writeAmountCell(totalRow, 3, styles.currency, totalNet);
        autoSize(sheet, 4);
    }

    private void renderRevenueSummary(Workbook workbook, ExcelStyles styles, ReportData data) {
        Sheet sheet = workbook.createSheet("Revenue");
        writeReportHeader(sheet, styles, data);
        int row = writeSectionTitle(sheet, styles, 4, "Revenue Overview");
        row = writeMetricRow(sheet, styles, row, "Total Revenue (UAH)", data.getRevenue());
        row = writeMetricRow(sheet, styles, row, "Profit (UAH)", data.getProfit());
        row += 1;
        row = writeCategoryTable(sheet, styles, row, "Revenue by Category", categories(data.getRevenueCategories()));
        autoSize(sheet, 2);
    }

    private void renderExpenseSummary(Workbook workbook, ExcelStyles styles, ReportData data) {
        Sheet sheet = workbook.createSheet("Expenses");
        writeReportHeader(sheet, styles, data);
        int row = writeSectionTitle(sheet, styles, 4, "Expense Overview");
        row = writeMetricRow(sheet, styles, row, "Total Expenses (UAH)", data.getExpenses());
        row = writeMetricRow(sheet, styles, row, "Tax Burden (UAH)", data.getTaxBurden());
        row += 1;
        row = writeCategoryTable(sheet, styles, row, "Expenses by Category", categories(data.getExpenseCategories()));
        autoSize(sheet, 2);
    }

    private void renderTaxSummary(Workbook workbook, ExcelStyles styles, ReportData data) {
        Sheet sheet = workbook.createSheet("Tax Summary");
        writeReportHeader(sheet, styles, data);
        int row = writeFopSection(sheet, styles, 4, data);
        row += 1;
        row = writeCategoryTable(sheet, styles, row, "Deductible Expenses by Category",
                categories(data.getExpenseCategories()));
        autoSize(sheet, 2);
    }

    private void renderFopSummary(Workbook workbook, ExcelStyles styles, ReportData data) {
        Sheet overview = workbook.createSheet("FOP Overview");
        writeReportHeader(overview, styles, data);
        int row = writeSectionTitle(overview, styles, 4, "Period Performance");
        row = writeMetricRow(overview, styles, row, "Revenue (UAH)", data.getRevenue());
        row = writeMetricRow(overview, styles, row, "Expenses (UAH)", data.getExpenses());
        row = writeMetricRow(overview, styles, row, "Profit (UAH)", data.getProfit());
        writeMetricRow(overview, styles, row, "Tax Burden (UAH)", data.getTaxBurden());
        autoSize(overview, 2);

        Sheet fop = workbook.createSheet("FOP Limits");
        writeReportHeader(fop, styles, data);
        writeFopSection(fop, styles, 4, data);
        autoSize(fop, 2);

        writeCategorySheet(workbook, styles, data, "Expenses", "Expenses by Category",
                categories(data.getExpenseCategories()));
    }

    private void writeCategorySheet(
            Workbook workbook,
            ExcelStyles styles,
            ReportData data,
            String sheetName,
            String sectionTitle,
            List<ReportData.CategoryLine> lines
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeReportHeader(sheet, styles, data);
        writeCategoryTable(sheet, styles, 4, sectionTitle, lines);
        autoSize(sheet, 2);
    }

    private void writeReportHeader(Sheet sheet, ExcelStyles styles, ReportData data) {
        Row brand = sheet.createRow(0);
        writeTextCell(brand, 0, styles.label, "Flowiq");

        Row title = sheet.createRow(1);
        writeTextCell(title, 0, styles.title, data.getTitle());

        Row period = sheet.createRow(2);
        writeTextCell(period, 0, styles.subtitle,
                "Period: " + data.getPeriodFrom() + " — " + data.getPeriodTo());

        Row generated = sheet.createRow(3);
        writeTextCell(generated, 0, styles.subtitle,
                "Generated: " + LocalDateTime.now().format(GENERATED_AT));
    }

    private int writeSectionTitle(Sheet sheet, ExcelStyles styles, int rowIdx, String title) {
        Row row = sheet.createRow(rowIdx);
        writeTextCell(row, 0, styles.label, title);
        return rowIdx + 1;
    }

    private int writeMetricRow(Sheet sheet, ExcelStyles styles, int rowIdx, String label, BigDecimal value) {
        Row row = sheet.createRow(rowIdx);
        writeTextCell(row, 0, styles.text, label);
        writeAmountCell(row, 1, styles.currency, value);
        return rowIdx + 1;
    }

    private int writeFopSection(Sheet sheet, ExcelStyles styles, int rowIdx, ReportData data) {
        int row = writeSectionTitle(sheet, styles, rowIdx, "FOP / Tax Profile");
        if (data.getFopGroup() == null) {
            Row empty = sheet.createRow(row);
            writeTextCell(empty, 0, styles.text, "No FOP profile data available");
            return row + 1;
        }

        row = writeTextMetricRow(sheet, styles, row, "FOP Group", data.getFopGroup());
        row = writePercentRow(sheet, styles, row, "Income Limit Usage", data.getIncomeLimitUsagePercent());
        row = writeMetricRow(sheet, styles, row, "Annual Income (UAH)", data.getAnnualIncome());
        row = writeMetricRow(sheet, styles, row, "Income Limit (UAH)", data.getIncomeLimit());
        row = writeMetricRow(sheet, styles, row, "Estimated Tax (UAH)", data.getEstimatedTax());
        return writeMetricRow(sheet, styles, row, "Tax Forecast (UAH)", data.getTaxForecast());
    }

    private int writeCategoryTable(
            Sheet sheet,
            ExcelStyles styles,
            int rowIdx,
            String title,
            List<ReportData.CategoryLine> lines
    ) {
        int row = writeSectionTitle(sheet, styles, rowIdx, title);

        Row header = sheet.createRow(row++);
        writeHeaderCell(header, 0, styles.header, "Category");
        writeHeaderCell(header, 1, styles.header, "Amount (UAH)");
        sheet.createFreezePane(0, row);

        BigDecimal total = BigDecimal.ZERO;
        for (ReportData.CategoryLine line : lines) {
            Row dataRow = sheet.createRow(row++);
            writeTextCell(dataRow, 0, styles.text, line.getCategory());
            writeAmountCell(dataRow, 1, styles.currency, line.getAmount());
            total = total.add(nullToZero(line.getAmount()));
        }

        Row totalRow = sheet.createRow(row);
        writeTextCell(totalRow, 0, styles.label, "Total");
        writeAmountCell(totalRow, 1, styles.currency, total);
        return row + 1;
    }

    private int writeTextMetricRow(Sheet sheet, ExcelStyles styles, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        writeTextCell(row, 0, styles.text, label);
        writeTextCell(row, 1, styles.text, value);
        return rowIdx + 1;
    }

    private int writePercentRow(Sheet sheet, ExcelStyles styles, int rowIdx, String label, double percent) {
        Row row = sheet.createRow(rowIdx);
        writeTextCell(row, 0, styles.text, label);
        Cell cell = row.createCell(1);
        cell.setCellStyle(styles.percent);
        cell.setCellValue(percent / 100.0);
        return rowIdx + 1;
    }

    private void writeHeaderCell(Row row, int col, CellStyle style, String value) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private void writeTextCell(Row row, int col, CellStyle style, String value) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(value != null ? value : "");
    }

    private void writeAmountCell(Row row, int col, CellStyle style, BigDecimal value) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(toDouble(value));
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 256 * 60));
        }
    }

    private static List<ReportData.CategoryLine> categories(List<ReportData.CategoryLine> lines) {
        return lines != null ? lines : List.of();
    }

    private static List<ReportData.MonthlyLine> monthly(List<ReportData.MonthlyLine> lines) {
        return lines != null ? lines : List.of();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static double toDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
