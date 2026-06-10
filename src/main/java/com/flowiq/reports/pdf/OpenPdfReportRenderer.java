package com.flowiq.reports.pdf;

import com.flowiq.exception.BadRequestException;
import com.flowiq.reports.ReportData;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Production PDF renderer using OpenPDF (LibrePDF).
 * Chosen over iText 7 for commercial SaaS: LGPL/MPL license without AGPL or paid seats.
 */
@Component
@RequiredArgsConstructor
public class OpenPdfReportRenderer {

    private static final Color BRAND = new Color(139, 92, 246);
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final PdfFontProvider fonts;

    public byte[] render(ReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 56, 56);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new ReportPageFooter(fonts));
            document.addTitle(data.getTitle());
            document.addCreator("Flowiq");
            document.addAuthor("Flowiq Reports");
            document.open();

            addHeader(document, data);
            addSummaryTable(document, data);
            addCategorySection(document, "Revenue by Category", data.getRevenueCategories());
            addCategorySection(document, "Expenses by Category", data.getExpenseCategories());
            addMonthlySection(document, data.getMonthlyLines());
            addFopSection(document, data);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new BadRequestException("Failed to generate PDF report: " + e.getMessage());
        }
    }

    private void addHeader(Document document, ReportData data) throws DocumentException {
        Paragraph brand = new Paragraph("Flowiq", fonts.bold(11));
        brand.setSpacingAfter(4);
        document.add(brand);

        Paragraph title = new Paragraph(data.getTitle(), fonts.bold(18));
        title.setSpacingAfter(6);
        document.add(title);

        Paragraph period = new Paragraph(
                "Period: " + data.getPeriodFrom() + " — " + data.getPeriodTo(),
                fonts.muted(10)
        );
        period.setSpacingAfter(16);
        document.add(period);
    }

    private void addSummaryTable(Document document, ReportData data) throws DocumentException {
        addSectionTitle(document, "Financial Summary");

        PdfPTable table = twoColumnTable();
        addHeaderRow(table, "Metric", "Amount");
        addDataRow(table, "Revenue", format(data.getRevenue()));
        addDataRow(table, "Expenses", format(data.getExpenses()));
        addDataRow(table, "Profit", format(data.getProfit()));
        addDataRow(table, "Tax Burden", format(data.getTaxBurden()));
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addCategorySection(
            Document document,
            String title,
            List<ReportData.CategoryLine> lines
    ) throws DocumentException {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        addSectionTitle(document, title);
        PdfPTable table = twoColumnTable();
        addHeaderRow(table, "Category", "Amount");
        for (ReportData.CategoryLine line : lines) {
            addDataRow(table, line.getCategory(), format(line.getAmount()));
        }
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addMonthlySection(Document document, List<ReportData.MonthlyLine> lines)
            throws DocumentException {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        addSectionTitle(document, "Monthly Breakdown");
        PdfPTable table = new PdfPTable(new float[]{2f, 2f, 2f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);
        addHeaderRow(table, "Month", "Revenue", "Expenses", "Profit");
        for (ReportData.MonthlyLine line : lines) {
            table.addCell(bodyCell(line.getMonth()));
            table.addCell(bodyCell(format(line.getRevenue())));
            table.addCell(bodyCell(format(line.getExpenses())));
            table.addCell(bodyCell(format(line.getProfit())));
        }
        document.add(table);
    }

    private void addFopSection(Document document, ReportData data) throws DocumentException {
        if (data.getFopGroup() == null) {
            return;
        }

        addSectionTitle(document, "FOP / Tax");
        PdfPTable table = twoColumnTable();
        addHeaderRow(table, "Field", "Value");
        addDataRow(table, "FOP Group", data.getFopGroup());
        addDataRow(table, "Income Limit Usage", String.format("%.1f%%", data.getIncomeLimitUsagePercent()));
        addDataRow(table, "Annual Income", format(data.getAnnualIncome()));
        addDataRow(table, "Income Limit", format(data.getIncomeLimit()));
        addDataRow(table, "Estimated Tax", format(data.getEstimatedTax()));
        addDataRow(table, "Tax Forecast", format(data.getTaxForecast()));
        document.add(table);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, fonts.bold(12));
        section.setSpacingBefore(4);
        section.setSpacingAfter(8);
        document.add(section);
    }

    private PdfPTable twoColumnTable() {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(100);
        return table;
    }

    private void addHeaderRow(PdfPTable table, String... headers) {
        for (String header : headers) {
            table.addCell(headerCell(header));
        }
    }

    private void addDataRow(PdfPTable table, String label, String value) {
        table.addCell(bodyCell(label));
        table.addCell(bodyCell(value, Element.ALIGN_RIGHT));
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fonts.bold(10)));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell bodyCell(String text) {
        return bodyCell(text, Element.ALIGN_LEFT);
    }

    private PdfPCell bodyCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fonts.regular(10)));
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        return cell;
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " UAH";
    }

    private static final class ReportPageFooter extends PdfPageEventHelper {
        private final PdfFontProvider fonts;
        private final String generatedAt = LocalDateTime.now().format(GENERATED_AT);

        private ReportPageFooter(PdfFontProvider fonts) {
            this.fonts = fonts;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font footerFont = fonts.muted(8);
            String left = "Generated by Flowiq · " + generatedAt;
            String right = "Page " + writer.getPageNumber();

            PdfPTable footer = new PdfPTable(2);
            footer.setTotalWidth(document.right() - document.left());
            footer.setWidths(new float[]{3f, 1f});

            PdfPCell leftCell = new PdfPCell(new Phrase(left, footerFont));
            leftCell.setBorder(PdfPCell.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);

            PdfPCell rightCell = new PdfPCell(new Phrase(right, footerFont));
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            footer.addCell(leftCell);
            footer.addCell(rightCell);
            footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());

            PdfContentByte canvas = writer.getDirectContent();
            canvas.setColorFill(BRAND);
            canvas.rectangle(document.left(), document.bottom() - 4, document.right() - document.left(), 1.5f);
            canvas.fill();
        }
    }
}
