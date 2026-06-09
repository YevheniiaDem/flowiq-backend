package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIInsightResponse {
    private String id;
    private String type;
    private String category;
    private String title;
    private String description;
    private String impact;
    private String timestamp;
    private String icon;
    private boolean actionable;
}
