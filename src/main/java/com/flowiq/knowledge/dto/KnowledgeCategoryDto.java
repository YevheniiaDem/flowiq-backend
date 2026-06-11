package com.flowiq.knowledge.dto;

import com.flowiq.knowledge.entity.KnowledgeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Knowledge category with article count")
public class KnowledgeCategoryDto {

    private KnowledgeCategory id;
    private String label;
    private long articleCount;
}
