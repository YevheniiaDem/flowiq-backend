package com.flowiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionSummaryResponse {

    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private long transactionCount;
}
