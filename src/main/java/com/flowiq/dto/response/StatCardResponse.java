package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard statistic card")
public class StatCardResponse {
    private String labelKey;
    private java.math.BigDecimal amount;
    private String change;
    private String changeType;
    private String icon;
}
