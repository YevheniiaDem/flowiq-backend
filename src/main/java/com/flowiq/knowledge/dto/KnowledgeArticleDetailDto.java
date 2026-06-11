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
@Schema(description = "Full knowledge article with related content")
public class KnowledgeArticleDetailDto {

    private Long id;
    private String slug;
    private String title;
    private KnowledgeCategory category;
    private String content;
    private String summary;
    private String impact;
    private List<String> tags;
    private LocalDate publishedAt;
    private LocalDateTime updatedAt;
    private int readingTimeMinutes;
    private List<KnowledgeArticleSummaryDto> relatedArticles;

    public static KnowledgeArticleDetailDto fromEntity(
            KnowledgeArticle article,
            boolean ukrainian,
            List<KnowledgeArticleSummaryDto> related
    ) {
        String content = ukrainian ? article.getContentUk() : article.getContentEn();
        String tagsRaw = ukrainian ? article.getTagsUk() : article.getTagsEn();
        List<String> tags = tagsRaw == null || tagsRaw.isBlank()
                ? List.of()
                : Arrays.stream(tagsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        return KnowledgeArticleDetailDto.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(ukrainian ? article.getTitleUk() : article.getTitleEn())
                .category(article.getCategory())
                .content(content)
                .summary(ukrainian ? article.getSummaryUk() : article.getSummaryEn())
                .impact(ukrainian ? article.getImpactUk() : article.getImpactEn())
                .tags(tags)
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .readingTimeMinutes(KnowledgeArticleSummaryDto.fromEntity(article, ukrainian).getReadingTimeMinutes())
                .relatedArticles(related)
                .build();
    }
}
