package com.flowiq.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Smart and AI-assisted knowledge search response")
public class KnowledgeSearchResponseDto {

    private String query;
    private KnowledgeArticleSummaryDto primaryArticle;
    private List<KnowledgeArticleSummaryDto> results;
    private List<KnowledgeArticleSummaryDto> relatedArticles;
    private String quickSummary;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
