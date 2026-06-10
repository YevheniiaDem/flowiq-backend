package com.flowiq.reports.excel;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

final class ExcelStyles {

    final CellStyle title;
    final CellStyle subtitle;
    final CellStyle header;
    final CellStyle label;
    final CellStyle currency;
    final CellStyle percent;
    final CellStyle text;

    private ExcelStyles(
            CellStyle title,
            CellStyle subtitle,
            CellStyle header,
            CellStyle label,
            CellStyle currency,
            CellStyle percent,
            CellStyle text
    ) {
        this.title = title;
        this.subtitle = subtitle;
        this.header = header;
        this.label = label;
        this.currency = currency;
        this.percent = percent;
        this.text = text;
    }

    static ExcelStyles create(Workbook workbook) {
        DataFormat format = workbook.createDataFormat();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        Font labelFont = workbook.createFont();
        labelFont.setBold(true);

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);

        CellStyle subtitle = workbook.createCellStyle();
        Font subtitleFont = workbook.createFont();
        subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subtitle.setFont(subtitleFont);

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);

        CellStyle label = workbook.createCellStyle();
        label.setFont(labelFont);

        CellStyle currency = workbook.createCellStyle();
        currency.setDataFormat(format.getFormat("#,##0.00"));
        currency.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle percent = workbook.createCellStyle();
        percent.setDataFormat(format.getFormat("0.0%"));
        percent.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle text = workbook.createCellStyle();

        return new ExcelStyles(title, subtitle, header, label, currency, percent, text);
    }
}
