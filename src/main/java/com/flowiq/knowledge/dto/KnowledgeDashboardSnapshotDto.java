package com.flowiq.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Business Guide dashboard widgets snapshot")
public class KnowledgeDashboardSnapshotDto {

    private List<KnowledgeArticleSummaryDto> popularArticles;
    private List<KnowledgeArticleSummaryDto> recentlyUpdated;
    private List<KnowledgeArticleSummaryDto> recommendedForYou;
    private List<KnowledgeArticleSummaryDto> latestLegalChanges;
}
