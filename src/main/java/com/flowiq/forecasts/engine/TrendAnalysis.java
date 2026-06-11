package com.flowiq.forecasts.engine;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrendAnalysis {
    double growthPercent;
    double monthlyGrowthRate;
}
