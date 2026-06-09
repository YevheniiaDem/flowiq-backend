package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatCardResponse {
    private String labelKey;
    private java.math.BigDecimal amount;
    private String change;
    private String changeType;
    private String icon;
}
