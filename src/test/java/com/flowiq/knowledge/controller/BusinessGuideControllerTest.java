package com.flowiq.knowledge.controller;

import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.knowledge.dto.KnowledgeArticleDetailDto;
import com.flowiq.knowledge.dto.KnowledgeArticlePageDto;
import com.flowiq.knowledge.dto.KnowledgeArticleSummaryDto;
import com.flowiq.knowledge.dto.KnowledgeCategoryDto;
import com.flowiq.knowledge.dto.KnowledgeDashboardSnapshotDto;
import com.flowiq.knowledge.dto.KnowledgeSearchResponseDto;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import com.flowiq.knowledge.service.KnowledgeService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessGuideController tests")
class BusinessGuideControllerTest {

    @Mock
    private KnowledgeService knowledgeService;

    @InjectMocks
    private BusinessGuideController businessGuideController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(businessGuideController);
    }

    @Test
    @DisplayName("GET /api/business-guide/articles returns paginated articles")
    void articles_success() throws Exception {
        when(knowledgeService.getArticles(0, 20, null, null)).thenReturn(
                KnowledgeArticlePageDto.builder()
                        .content(List.of(KnowledgeArticleSummaryDto.builder().slug("fop-group-2").title("FOP Group 2").build()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .build());

        mockMvc.perform(get("/api/business-guide/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("fop-group-2"));
    }

    @Test
    @DisplayName("GET /api/business-guide/articles/{slug} returns article detail")
    void articleBySlug_success() throws Exception {
        when(knowledgeService.getArticleBySlug("fop-group-2")).thenReturn(
                KnowledgeArticleDetailDto.builder()
                        .slug("fop-group-2")
                        .title("FOP Group 2")
                        .category(KnowledgeCategory.FOP_GROUPS)
                        .build());

        mockMvc.perform(get("/api/business-guide/articles/fop-group-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("FOP Group 2"));
    }

    @Test
    @DisplayName("GET /api/business-guide/articles/{slug} returns 404 when not found")
    void articleBySlug_notFound() throws Exception {
        when(knowledgeService.getArticleBySlug("missing"))
                .thenThrow(new ResourceNotFoundException("Article not found"));

        mockMvc.perform(get("/api/business-guide/articles/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/business-guide/categories returns categories")
    void categories_success() throws Exception {
        when(knowledgeService.getCategories()).thenReturn(List.of(
                KnowledgeCategoryDto.builder()
                        .id(KnowledgeCategory.TAXES)
                        .label("Taxes")
                        .articleCount(5)
                        .build()));

        mockMvc.perform(get("/api/business-guide/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Taxes"));
    }

    @Test
    @DisplayName("GET /api/business-guide/search returns search results")
    void search_success() throws Exception {
        when(knowledgeService.search(eq("esv"), eq(0), eq(20), isNull())).thenReturn(
                KnowledgeSearchResponseDto.builder()
                        .query("esv")
                        .quickSummary("ESV is a unified social contribution.")
                        .results(List.of())
                        .totalElements(0)
                        .build());

        mockMvc.perform(get("/api/business-guide/search").param("q", "esv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("esv"));
    }

    @Test
    @DisplayName("GET /api/business-guide/dashboard-snapshot returns dashboard data")
    void dashboardSnapshot_success() throws Exception {
        when(knowledgeService.getDashboardSnapshot()).thenReturn(
                KnowledgeDashboardSnapshotDto.builder()
                        .popularArticles(List.of())
                        .recentlyUpdated(List.of())
                        .recommendedForYou(List.of())
                        .latestLegalChanges(List.of())
                        .build());

        mockMvc.perform(get("/api/business-guide/dashboard-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularArticles").isArray());
    }

    @Test
    @DisplayName("GET /api/business-guide/articles returns 401 when unauthorized")
    void articles_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated"))
                .when(knowledgeService)
                .getArticles(0, 20, null, null);

        mockMvc.perform(get("/api/business-guide/articles"))
                .andExpect(status().isUnauthorized());
    }
}
