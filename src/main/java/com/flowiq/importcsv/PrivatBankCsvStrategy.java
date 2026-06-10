package com.flowiq.importcsv;

import com.flowiq.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PrivatBankCsvStrategy implements CsvImportStrategy {

    private static final Set<String> INCOME_CATEGORIES = Set.of(
            "services", "consulting", "software", "sales", "subscription", "other"
    );
    private static final Set<String> EXPENSE_CATEGORIES = Set.of(
            "marketing", "salary", "infrastructure", "equipment", "office", "taxes", "software", "other"
    );

    @Override
    public boolean supports(String[] headers) {
        String joined = String.join(" ", headers).toLowerCase();
        return joined.contains("картка") || joined.contains("card")
                || (joined.contains("privat") && joined.contains("опис"));
    }

    @Override
    public String getBankName() {
        return "PrivatBank";
    }

    @Override
    public List<ParsedTransactionRow> parse(String csvContent) {
        List<String[]> lines = CsvLineParser.parseLines(csvContent);
        if (lines.isEmpty()) {
            return List.of();
        }

        String[] headers = CsvLineParser.normalizeHeaders(lines.get(0));
        int dateCol = CsvLineParser.findColumn(headers, "дата", "date");
        int categoryCol = CsvLineParser.findColumn(headers, "категорія", "category");
        int descCol = CsvLineParser.findColumn(headers, "опис", "description");
        int amountCol = CsvLineParser.findColumn(headers, "сума в гривнях", "amount (uah)", "сума");

        if (dateCol < 0 || amountCol < 0) {
            throw new CsvParseException("PrivatBank CSV: required columns not found");
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

                String bankCategory = CsvLineParser.cell(cells, categoryCol);
                String category = mapCategory(bankCategory, type);
                String description = CsvLineParser.cell(cells, descCol);

                rows.add(ParsedTransactionRow.builder()
                        .transactionDate(CsvLineParser.parseDate(CsvLineParser.cell(cells, dateCol)))
                        .type(type)
                        .amount(amount)
                        .category(category)
                        .description(description.isBlank() ? null : description)
                        .build());
            } catch (CsvParseException e) {
                // skip invalid rows
            }
        }
        return rows;
    }

    private String mapCategory(String bankCategory, Transaction.Type type) {
        if (bankCategory == null || bankCategory.isBlank()) {
            return "Other";
        }
        String normalized = bankCategory.trim();
        for (String allowed : type == Transaction.Type.REVENUE ? INCOME_CATEGORIES : EXPENSE_CATEGORIES) {
            if (allowed.equalsIgnoreCase(normalized)) {
                return capitalize(allowed);
            }
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("зарплат") || lower.contains("salary")) {
            return "Salary";
        }
        if (lower.contains("подат") || lower.contains("tax")) {
            return "Taxes";
        }
        if (lower.contains("офіс") || lower.contains("office")) {
            return "Office";
        }
        return "Other";
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
