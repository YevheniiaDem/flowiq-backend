package com.flowiq.knowledge.provider;

import com.flowiq.knowledge.dto.KnowledgeArticleSummaryDto;
import com.flowiq.knowledge.entity.KnowledgeArticle;

import java.util.List;

/**
 * Extension point for AI-assisted knowledge search.
 * {@link com.flowiq.knowledge.provider.DatabaseKnowledgeProvider} uses the local database;
 * future integrations (OpenAI, Claude, Gemini) can implement this interface as Spring beans.
 */
public interface KnowledgeProvider {

    KnowledgeAssistResult assistSearch(KnowledgeSearchContext context);

    record KnowledgeSearchContext(
            String query,
            boolean ukrainian,
            List<KnowledgeArticle> candidates,
            KnowledgeArticle primaryArticle
    ) {}

    record KnowledgeAssistResult(
            String quickSummary,
            KnowledgeArticleSummaryDto primaryArticle,
            List<KnowledgeArticleSummaryDto> relatedArticles
    ) {}
}
