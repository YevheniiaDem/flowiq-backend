package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPreviewResponse {

    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal profit;
    private BigDecimal taxBurden;
    private List<MonthlyAmountResponse> chartData;
}
