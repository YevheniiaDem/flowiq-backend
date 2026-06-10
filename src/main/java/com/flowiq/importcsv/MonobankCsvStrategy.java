package com.flowiq.importcsv;

import com.flowiq.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class MonobankCsvStrategy implements CsvImportStrategy {

    @Override
    public boolean supports(String[] headers) {
        String joined = String.join(" ", headers).toLowerCase();
        return joined.contains("мсс") || joined.contains("mcc")
                || (joined.contains("monobank") && joined.contains("сума"));
    }

    @Override
    public String getBankName() {
        return "Monobank";
    }

    @Override
    public List<ParsedTransactionRow> parse(String csvContent) {
        List<String[]> lines = CsvLineParser.parseLines(csvContent);
        if (lines.isEmpty()) {
            return List.of();
        }

        String[] headers = CsvLineParser.normalizeHeaders(lines.get(0));
        int dateCol = CsvLineParser.findColumn(headers, "дата", "date");
        int descCol = CsvLineParser.findColumn(headers, "опис", "description");
        int amountCol = CsvLineParser.findColumn(headers, "сума в гривнях", "amount (uah)", "сума");
        int mccCol = CsvLineParser.findColumn(headers, "мсс", "mcc");

        if (dateCol < 0 || amountCol < 0) {
            throw new CsvParseException("Monobank CSV: required columns not found");
        }

        List<ParsedTransactionRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] cells = lines.get(i);
            try {
                BigDecimal rawAmount = CsvLineParser.parseAmount(CsvLineParser.cell(cells, amountCol));
                if (rawAmount.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                Transaction.Type type = rawAmount.signum() >= 0
                        ? Transaction.Type.REVENUE
                        : Transaction.Type.EXPENSE;
                BigDecimal amount = rawAmount.abs();

                String description = CsvLineParser.cell(cells, descCol);
                String mcc = CsvLineParser.cell(cells, mccCol);
                String category = mapCategory(mcc, type);

                rows.add(ParsedTransactionRow.builder()
                        .transactionDate(CsvLineParser.parseDate(CsvLineParser.cell(cells, dateCol)))
                        .type(type)
                        .amount(amount)
                        .category(category)
                        .description(description.isBlank() ? null : description)
                        .build());
            } catch (CsvParseException e) {
                // skip invalid rows; counted as errors in service
            }
        }
        return rows;
    }

    private String mapCategory(String mcc, Transaction.Type type) {
        if (type == Transaction.Type.REVENUE) {
            return "Other";
        }
        if (mcc == null || mcc.isBlank()) {
            return "Other";
        }
        if (mcc.startsWith("54") || mcc.startsWith("53")) {
            return "Office";
        }
        if (mcc.startsWith("41") || mcc.startsWith("42")) {
            return "Infrastructure";
        }
        return "Other";
    }
}
