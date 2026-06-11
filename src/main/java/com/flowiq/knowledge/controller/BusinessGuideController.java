package com.flowiq.knowledge.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.knowledge.dto.*;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import com.flowiq.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Business Guide", description = "Knowledge base for Ukrainian FOP entrepreneurs")
@RestController
@RequestMapping("/api/business-guide")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BusinessGuideController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "List knowledge articles", description = "Returns paginated articles with optional category and tag filters.")
    @ApiResponse(responseCode = "200", description = "Paginated articles",
            content = @Content(schema = @Schema(implementation = KnowledgeArticlePageDto.class)))
    @ApiErrorResponses
    @GetMapping("/articles")
    public ResponseEntity<KnowledgeArticlePageDto> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) KnowledgeCategory category,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(knowledgeService.getArticles(page, size, category, tag));
    }

    @Operation(summary = "Get article by slug", description = "Returns full article content with related articles.")
    @ApiResponse(responseCode = "200", description = "Article details",
            content = @Content(schema = @Schema(implementation = KnowledgeArticleDetailDto.class)))
    @ApiErrorResponses
    @GetMapping("/articles/{slug}")
    public ResponseEntity<KnowledgeArticleDetailDto> getArticle(
            @Parameter(description = "SEO-friendly article slug") @PathVariable String slug
    ) {
        return ResponseEntity.ok(knowledgeService.getArticleBySlug(slug));
    }

    @Operation(summary = "List knowledge categories", description = "Returns all categories with article counts.")
    @ApiResponse(responseCode = "200", description = "Category list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = KnowledgeCategoryDto.class))))
    @ApiErrorResponses
    @GetMapping("/categories")
    public ResponseEntity<List<KnowledgeCategoryDto>> getCategories() {
        return ResponseEntity.ok(knowledgeService.getCategories());
    }

    @Operation(summary = "Smart knowledge search", description = "Searches by title, keyword, tag, KVED, tax type, and FOP group with AI-assisted summary.")
    @ApiResponse(responseCode = "200", description = "Search results with quick summary",
            content = @Content(schema = @Schema(implementation = KnowledgeSearchResponseDto.class)))
    @ApiErrorResponses
    @GetMapping("/search")
    public ResponseEntity<KnowledgeSearchResponseDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) KnowledgeCategory category
    ) {
        return ResponseEntity.ok(knowledgeService.search(q, page, size, category));
    }

    @Operation(summary = "Business Guide dashboard snapshot", description = "Returns popular, recent, recommended articles and legal updates.")
    @ApiResponse(responseCode = "200", description = "Dashboard widgets data",
            content = @Content(schema = @Schema(implementation = KnowledgeDashboardSnapshotDto.class)))
    @ApiErrorResponses
    @GetMapping("/dashboard-snapshot")
    public ResponseEntity<KnowledgeDashboardSnapshotDto> getDashboardSnapshot() {
        return ResponseEntity.ok(knowledgeService.getDashboardSnapshot());
    }
}
