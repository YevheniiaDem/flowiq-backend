package com.flowiq.importcsv;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CsvImportStrategyResolver {

    private final MonobankCsvStrategy monobankCsvStrategy;
    private final PrivatBankCsvStrategy privatBankCsvStrategy;
    private final UniversalCsvStrategy universalCsvStrategy;

    public CsvImportStrategy resolve(String csvContent) {
        List<String[]> lines = CsvLineParser.parseLines(csvContent);
        if (lines.isEmpty()) {
            throw new CsvParseException("CSV file is empty");
        }

        String[] headers = CsvLineParser.normalizeHeaders(lines.get(0));

        if (monobankCsvStrategy.supports(headers)) {
            return monobankCsvStrategy;
        }
        if (privatBankCsvStrategy.supports(headers)) {
            return privatBankCsvStrategy;
        }
        if (universalCsvStrategy.supports(headers)) {
            return universalCsvStrategy;
        }

        throw new CsvParseException("Unsupported CSV format. Expected Monobank, PrivatBank, or Universal format.");
    }
}
