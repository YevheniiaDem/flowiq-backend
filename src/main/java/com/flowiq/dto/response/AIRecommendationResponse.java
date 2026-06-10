package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendationResponse {

    private String id;
    private String type;
    private String title;
    private String description;
}
