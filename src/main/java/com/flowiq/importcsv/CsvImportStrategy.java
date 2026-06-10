package com.flowiq.importcsv;

import java.util.List;

public interface CsvImportStrategy {

    boolean supports(String[] headers);

    String getBankName();

    List<ParsedTransactionRow> parse(String csvContent);
}
