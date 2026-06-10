package com.flowiq.analytics;

import com.flowiq.dto.response.AnalyticsInsightResponse;

import java.util.List;

/**
 * Extension point for AI-powered analytics insights.
 * Rule-based insights are generated in {@link com.flowiq.service.AnalyticsService};
 * future AI providers can implement this interface as Spring beans.
 */
public interface AnalyticsInsightProvider {

    List<AnalyticsInsightResponse> getInsights(Long userId);
}
