package com.flowiq.knowledge.provider;

import com.flowiq.knowledge.dto.KnowledgeArticleSummaryDto;
import com.flowiq.knowledge.entity.KnowledgeArticle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class DatabaseKnowledgeProvider implements KnowledgeProvider {

    @Override
    public KnowledgeAssistResult assistSearch(KnowledgeSearchContext context) {
        KnowledgeArticle resolvedPrimary = context.primaryArticle();
        if (resolvedPrimary == null && !context.candidates().isEmpty()) {
            resolvedPrimary = rankCandidates(context.query(), context.candidates()).get(0);
        }
        final KnowledgeArticle primary = resolvedPrimary;

        if (primary == null) {
            return new KnowledgeAssistResult(
                    context.ukrainian()
                            ? "Статей за вашим запитом не знайдено. Спробуйте інші ключові слова."
                            : "No articles found for your query. Try different keywords.",
                    null,
                    List.of()
            );
        }

        boolean ukrainian = context.ukrainian();
        KnowledgeArticleSummaryDto primaryDto = KnowledgeArticleSummaryDto.fromEntity(primary, ukrainian);
        List<KnowledgeArticleSummaryDto> related = context.candidates().stream()
                .filter(article -> !article.getSlug().equals(primary.getSlug()))
                .sorted(Comparator.comparingInt((KnowledgeArticle a) -> score(a, context.query())).reversed())
                .limit(4)
                .map(article -> KnowledgeArticleSummaryDto.fromEntity(article, ukrainian))
                .toList();

        String summary = buildQuickSummary(primary, ukrainian, related);
        return new KnowledgeAssistResult(summary, primaryDto, related);
    }

    private List<KnowledgeArticle> rankCandidates(String query, List<KnowledgeArticle> candidates) {
        List<KnowledgeArticle> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparingInt((KnowledgeArticle a) -> score(a, query)).reversed());
        return ranked;
    }

    private int score(KnowledgeArticle article, String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return article.getViewCount();
        }

        int score = 0;
        score += containsScore(article.getTitleUk(), normalized, 12);
        score += containsScore(article.getTitleEn(), normalized, 12);
        score += containsScore(article.getTagsUk(), normalized, 10);
        score += containsScore(article.getTagsEn(), normalized, 10);
        score += containsScore(article.getSummaryUk(), normalized, 8);
        score += containsScore(article.getSummaryEn(), normalized, 8);
        score += containsScore(article.getContentUk(), normalized, 3);
        score += containsScore(article.getContentEn(), normalized, 3);

        score += fopGroupBoost(article, normalized);
        score += taxTypeBoost(article, normalized);
        score += kvedBoost(article, normalized);
        score += article.getViewCount();
        return score;
    }

    private int containsScore(String text, String query, int weight) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.toLowerCase(Locale.ROOT).contains(query) ? weight : 0;
    }

    private int fopGroupBoost(KnowledgeArticle article, String query) {
        if (query.contains("груп") || query.contains("group") || query.contains("фоп")) {
            if (query.contains("1") && article.getSlug().contains("group-1")) return 20;
            if (query.contains("2") && article.getSlug().contains("group-2")) return 20;
            if (query.contains("3") && article.getSlug().contains("group-3")) return 20;
        }
        return 0;
    }

    private int taxTypeBoost(KnowledgeArticle article, String query) {
        if (query.contains("єсв") || query.contains("esv")) {
            if (article.getCategory().name().equals("ESV")) return 25;
        }
        if (query.contains("військов") || query.contains("military")) {
            if (article.getCategory().name().equals("MILITARY_TAX")) return 25;
        }
        if (query.contains("єдин") || query.contains("unified") || query.contains("подат")) {
            if (article.getCategory().name().equals("TAXES")) return 15;
        }
        return 0;
    }

    private int kvedBoost(KnowledgeArticle article, String query) {
        if (query.matches(".*\\d{2}\\.\\d{2}.*")) {
            String kved = query.replaceAll(".*(\\d{2}\\.\\d{2}).*", "$1");
            if ((article.getTagsUk() != null && article.getTagsUk().contains(kved))
                    || (article.getTagsEn() != null && article.getTagsEn().contains(kved))
                    || (article.getContentUk() != null && article.getContentUk().contains(kved))) {
                return 30;
            }
        }
        if (query.contains("квед") || query.contains("kved")) {
            if (article.getCategory().name().equals("KVED_DIRECTORY")) return 20;
        }
        return 0;
    }

    private String buildQuickSummary(
            KnowledgeArticle primary,
            boolean ukrainian,
            List<KnowledgeArticleSummaryDto> related
    ) {
        String summary = ukrainian ? primary.getSummaryUk() : primary.getSummaryEn();
        if (summary != null && !summary.isBlank()) {
            StringBuilder builder = new StringBuilder(summary);
            if (!related.isEmpty()) {
                builder.append(ukrainian ? " Див. також: " : " See also: ");
                builder.append(related.stream()
                        .map(KnowledgeArticleSummaryDto::getTitle)
                        .limit(2)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));
                builder.append(".");
            }
            return builder.toString();
        }
        String content = ukrainian ? primary.getContentUk() : primary.getContentEn();
        if (content == null || content.isBlank()) {
            return ukrainian ? "Короткий опис недоступний." : "Summary unavailable.";
        }
        return content.length() > 280 ? content.substring(0, 277).trim() + "…" : content;
    }
}
