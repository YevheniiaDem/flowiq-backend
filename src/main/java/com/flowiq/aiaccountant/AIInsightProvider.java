package com.flowiq.aiaccountant;

import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.AIRecommendationResponse;

import java.util.List;
import java.util.Optional;

/**
 * Extension point for external AI backends (OpenAI, Claude, Gemini).
 * Rule-based logic lives in {@link AIRecommendationEngine} and {@link AIAccountantService}.
 */
public interface AIInsightProvider {

    List<AIRecommendationResponse> getRecommendations(FinancialSnapshot snapshot);

    Optional<AIAccountantChatResponse> answerChat(FinancialSnapshot snapshot, String message);
}
