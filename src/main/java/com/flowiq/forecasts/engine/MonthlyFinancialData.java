package com.flowiq.forecasts.engine;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class MonthlyFinancialData {
    String month;
    BigDecimal revenue;
    BigDecimal expenses;

    public BigDecimal getProfit() {
        return revenue.subtract(expenses);
    }
}
