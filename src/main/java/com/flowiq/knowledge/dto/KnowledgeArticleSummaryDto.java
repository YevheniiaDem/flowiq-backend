package com.flowiq.knowledge.dto;

import com.flowiq.knowledge.entity.KnowledgeArticle;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@Schema(description = "Knowledge article summary for lists and search results")
public class KnowledgeArticleSummaryDto {

    private Long id;
    private String slug;
    private String title;
    private KnowledgeCategory category;
    private String summary;
    private List<String> tags;
    private LocalDate publishedAt;
    private LocalDateTime updatedAt;
    private int readingTimeMinutes;
    private String highlight;
    private String impact;

    public static KnowledgeArticleSummaryDto fromEntity(KnowledgeArticle article, boolean ukrainian) {
        String title = ukrainian ? article.getTitleUk() : article.getTitleEn();
        String summary = ukrainian ? article.getSummaryUk() : article.getSummaryEn();
        String tagsRaw = ukrainian ? article.getTagsUk() : article.getTagsEn();
        List<String> tags = tagsRaw == null || tagsRaw.isBlank()
                ? List.of()
                : Arrays.stream(tagsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        String content = ukrainian ? article.getContentUk() : article.getContentEn();

        return KnowledgeArticleSummaryDto.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(title)
                .category(article.getCategory())
                .summary(summary)
                .tags(tags)
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .readingTimeMinutes(estimateReadingTime(content))
                .impact(ukrainian ? article.getImpactUk() : article.getImpactEn())
                .build();
    }

    public static KnowledgeArticleSummaryDto fromEntityWithHighlight(
            KnowledgeArticle article, boolean ukrainian, String query
    ) {
        KnowledgeArticleSummaryDto dto = fromEntity(article, ukrainian);
        if (query != null && !query.isBlank()) {
            dto.setHighlight(buildHighlight(dto.getTitle(), dto.getSummary(), query));
        }
        return dto;
    }

    private static int estimateReadingTime(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    private static String buildHighlight(String title, String summary, String query) {
        String normalized = query.trim().toLowerCase();
        String source = title != null ? title : "";
        if (summary != null && summary.toLowerCase().contains(normalized)) {
            source = summary;
        }
        int index = source.toLowerCase().indexOf(normalized);
        if (index < 0) {
            return summary != null && !summary.isBlank() ? summary : title;
        }
        int start = Math.max(0, index - 40);
        int end = Math.min(source.length(), index + normalized.length() + 60);
        String excerpt = source.substring(start, end).trim();
        if (start > 0) {
            excerpt = "…" + excerpt;
        }
        if (end < source.length()) {
            excerpt = excerpt + "…";
        }
        return excerpt;
    }
}
