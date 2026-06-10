package com.flowiq.importcsv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CsvLineParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    );

    private CsvLineParser() {
    }

    static List<String[]> parseLines(String csvContent) {
        List<String[]> lines = new ArrayList<>();
        for (String line : csvContent.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            lines.add(parseLine(line));
        }
        return lines;
    }

    static String[] parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString().trim());
        return cells.toArray(new String[0]);
    }

    static String[] normalizeHeaders(String[] headers) {
        String[] normalized = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            normalized[i] = headers[i].trim().toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    static int findColumn(String[] headers, String... candidates) {
        for (String candidate : candidates) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].contains(lower)) {
                    return i;
                }
            }
        }
        return -1;
    }

    static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new CsvParseException("Date is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 10 && trimmed.charAt(4) == '-') {
            try {
                return LocalDate.parse(trimmed.substring(0, 10));
            } catch (DateTimeParseException ignored) {
                // try other formatters
            }
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter.toString().contains("HH")) {
                    return LocalDateTime.parse(trimmed, formatter).toLocalDate();
                }
                return LocalDate.parse(trimmed.length() > 10 ? trimmed.substring(0, 10) : trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new CsvParseException("Unsupported date format: " + value);
    }

    static BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            throw new CsvParseException("Amount is required");
        }
        String normalized = value.trim()
                .replace(" ", "")
                .replace("\u00a0", "")
                .replace(",", ".");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new CsvParseException("Invalid amount: " + value, e);
        }
    }

    static String cell(String[] row, int index) {
        if (index < 0 || index >= row.length) {
            return "";
        }
        return row[index].trim();
    }
}
