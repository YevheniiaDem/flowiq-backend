package com.flowiq.importcsv;

import com.flowiq.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class UniversalCsvStrategy implements CsvImportStrategy {

    private static final Set<String> INCOME_CATEGORIES = Set.of(
            "Services", "Consulting", "Software", "Sales", "Subscription", "Other"
    );
    private static final Set<String> EXPENSE_CATEGORIES = Set.of(
            "Marketing", "Salary", "Infrastructure", "Equipment", "Office", "Taxes", "Software", "Other"
    );

    @Override
    public boolean supports(String[] headers) {
        String joined = String.join(" ", headers).toLowerCase(Locale.ROOT);
        return joined.contains("type") && joined.contains("category") && joined.contains("amount");
    }

    @Override
    public String getBankName() {
        return "Universal";
    }

    @Override
    public List<ParsedTransactionRow> parse(String csvContent) {
        List<String[]> lines = CsvLineParser.parseLines(csvContent);
        if (lines.isEmpty()) {
            return List.of();
        }

        String[] headers = CsvLineParser.normalizeHeaders(lines.get(0));
        int dateCol = CsvLineParser.findColumn(headers, "date", "дата");
        int typeCol = CsvLineParser.findColumn(headers, "type", "тип");
        int categoryCol = CsvLineParser.findColumn(headers, "category", "категорія");
        int descCol = CsvLineParser.findColumn(headers, "description", "опис");
        int amountCol = CsvLineParser.findColumn(headers, "amount", "сума");

        if (dateCol < 0 || typeCol < 0 || categoryCol < 0 || amountCol < 0) {
            throw new CsvParseException("Universal CSV: required columns not found");
        }

        List<ParsedTransactionRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] cells = lines.get(i);
            try {
                String typeRaw = CsvLineParser.cell(cells, typeCol).toUpperCase(Locale.ROOT);
                Transaction.Type type = typeRaw.contains("EXPENSE") || typeRaw.contains("ВИТРАТ")
                        ? Transaction.Type.EXPENSE
                        : Transaction.Type.REVENUE;

                String category = CsvLineParser.cell(cells, categoryCol);
                validateCategory(type, category);

                BigDecimal amount = CsvLineParser.parseAmount(CsvLineParser.cell(cells, amountCol)).abs();
                String description = CsvLineParser.cell(cells, descCol);

                rows.add(ParsedTransactionRow.builder()
                        .transactionDate(CsvLineParser.parseDate(CsvLineParser.cell(cells, dateCol)))
                        .type(type)
                        .amount(amount)
                        .category(category.trim())
                        .description(description.isBlank() ? null : description)
                        .build());
            } catch (CsvParseException e) {
                // skip invalid rows
            }
        }
        return rows;
    }

    private void validateCategory(Transaction.Type type, String category) {
        Set<String> allowed = type == Transaction.Type.REVENUE ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        if (category == null || category.isBlank() || !allowed.contains(category.trim())) {
            throw new CsvParseException("Invalid category: " + category);
        }
    }
}
