package com.flowiq.importcsv;

import com.flowiq.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTransactionRow {

    private LocalDate transactionDate;
    private Transaction.Type type;
    private BigDecimal amount;
    private String category;
    private String description;
}
