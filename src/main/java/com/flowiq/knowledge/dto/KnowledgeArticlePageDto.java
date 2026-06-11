package com.flowiq.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Paginated knowledge articles")
public class KnowledgeArticlePageDto {

    private List<KnowledgeArticleSummaryDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
