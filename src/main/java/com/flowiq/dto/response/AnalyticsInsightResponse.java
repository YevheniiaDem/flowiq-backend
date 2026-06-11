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
@Schema(description = "Analytics insight item")
public class AnalyticsInsightResponse {

    private String id;
    private String type;
    private String title;
    private String description;
}
