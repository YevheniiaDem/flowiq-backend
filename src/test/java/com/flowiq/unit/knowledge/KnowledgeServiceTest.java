package com.flowiq.unit.knowledge;

import com.flowiq.config.AppPreferences;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.knowledge.dto.*;
import com.flowiq.knowledge.entity.KnowledgeArticle;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import com.flowiq.knowledge.provider.DatabaseKnowledgeProvider;
import com.flowiq.knowledge.repository.KnowledgeArticleRepository;
import com.flowiq.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KnowledgeService unit tests")
class KnowledgeServiceTest {

    @Mock
    private KnowledgeArticleRepository repository;

    @Spy
    private DatabaseKnowledgeProvider databaseKnowledgeProvider = new DatabaseKnowledgeProvider();

    @InjectMocks
    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        AppPreferences.clear();
    }

    @AfterEach
    void tearDown() {
        AppPreferences.clear();
    }

    @Test
    @DisplayName("getArticles returns paginated summaries")
    void getArticles_happyPath() {
        KnowledgeArticle article = sampleArticle("tax-guide", KnowledgeCategory.TAXES);
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(article));
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        KnowledgeArticlePageDto result = knowledgeService.getArticles(0, 10, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSlug()).isEqualTo("tax-guide");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getArticles filters by tag in Ukrainian locale")
    void getArticles_tagFilter_ukrainian() {
        AppPreferences prefs = new AppPreferences();
        prefs.setLanguage("uk");
        AppPreferences.set(prefs);

        KnowledgeArticle matching = sampleArticle("esv-rules", KnowledgeCategory.ESV);
        matching.setTagsUk("єсв,податки");
        KnowledgeArticle nonMatching = sampleArticle("other", KnowledgeCategory.TAXES);
        nonMatching.setTagsUk("інше");

        Page<KnowledgeArticle> page = new PageImpl<>(List.of(matching, nonMatching));
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        KnowledgeArticlePageDto result = knowledgeService.getArticles(0, 10, null, "єсв");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSlug()).isEqualTo("esv-rules");
    }

    @Test
    @DisplayName("getArticles filters by category")
    void getArticles_categoryFilter() {
        KnowledgeArticle article = sampleArticle("fop-2", KnowledgeCategory.FOP_GROUPS);
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(article));
        when(repository.findByCategory(eq(KnowledgeCategory.FOP_GROUPS), any(Pageable.class)))
                .thenReturn(page);

        KnowledgeArticlePageDto result = knowledgeService.getArticles(0, 5, KnowledgeCategory.FOP_GROUPS, null);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByCategory(eq(KnowledgeCategory.FOP_GROUPS), any(Pageable.class));
    }

    @Test
    @DisplayName("getArticleBySlug increments view count and returns detail")
    void getArticleBySlug_happyPath() {
        KnowledgeArticle article = sampleArticle("unified-tax", KnowledgeCategory.TAXES);
        article.setViewCount(5);
        when(repository.findBySlug("unified-tax")).thenReturn(Optional.of(article));
        when(repository.findByCategory(eq(KnowledgeCategory.TAXES), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        KnowledgeArticleDetailDto detail = knowledgeService.getArticleBySlug("unified-tax");

        assertThat(detail.getSlug()).isEqualTo("unified-tax");
        assertThat(article.getViewCount()).isEqualTo(6);
        verify(repository).save(article);
    }

    @Test
    @DisplayName("getArticleBySlug throws when slug not found")
    void getArticleBySlug_notFound() {
        when(repository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.getArticleBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("getCategories returns all categories with counts")
    void getCategories_returnsAll() {
        when(repository.findByCategory(any(KnowledgeCategory.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleArticle("x", KnowledgeCategory.TAXES))));

        List<KnowledgeCategoryDto> categories = knowledgeService.getCategories();

        assertThat(categories).hasSize(KnowledgeCategory.values().length);
        assertThat(categories.get(0).getLabel()).isNotBlank();
    }

    @Test
    @DisplayName("search with blank query returns all articles")
    void search_blankQuery_returnsAll() {
        KnowledgeArticle article = sampleArticle("faq-1", KnowledgeCategory.BUSINESS_FAQ);
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article)));

        KnowledgeSearchResponseDto response = knowledgeService.search("   ", 0, 10, null);

        assertThat(response.getResults()).hasSize(1);
        verify(repository).findAll(any(Pageable.class));
        verify(repository, never()).search(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search with query delegates to repository search")
    void search_withQuery_searchesRepository() {
        KnowledgeArticle article = sampleArticle("esv-payment", KnowledgeCategory.ESV);
        when(repository.search(eq("єсв"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article)));

        KnowledgeSearchResponseDto response = knowledgeService.search("єсв", 0, 10, null);

        assertThat(response.getQuery()).isEqualTo("єсв");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getPrimaryArticle()).isNotNull();
    }

    @Test
    @DisplayName("search with category and query uses category search")
    void search_withCategory_usesCategorySearch() {
        when(repository.searchByCategory(eq("податок"), eq(KnowledgeCategory.TAXES), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        KnowledgeSearchResponseDto response = knowledgeService.search("податок", 0, 5, KnowledgeCategory.TAXES);

        assertThat(response.getResults()).isEmpty();
        verify(repository).searchByCategory(eq("податок"), eq(KnowledgeCategory.TAXES), any(Pageable.class));
    }

    @Test
    @DisplayName("search returns empty assist message when no results")
    void search_noResults_emptyAssist() {
        when(repository.search(eq("xyz"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        KnowledgeSearchResponseDto response = knowledgeService.search("xyz", 0, 10, null);

        assertThat(response.getResults()).isEmpty();
        assertThat(response.getQuickSummary()).contains("не знайдено");
    }

    @Test
    @DisplayName("getDashboardSnapshot aggregates popular, recent, legal and recommended")
    void getDashboardSnapshot_happyPath() {
        KnowledgeArticle popular = sampleArticle("popular", KnowledgeCategory.TAXES);
        when(repository.findTop5ByOrderByViewCountDesc()).thenReturn(List.of(popular));
        when(repository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of(popular));
        when(repository.findByCategoryOrderByPublishedAtDesc(KnowledgeCategory.LEGAL_CHANGES))
                .thenReturn(List.of(popular));
        when(repository.findBySlug(anyString())).thenReturn(Optional.empty());

        KnowledgeDashboardSnapshotDto snapshot = knowledgeService.getDashboardSnapshot();

        assertThat(snapshot.getPopularArticles()).hasSize(1);
        assertThat(snapshot.getRecentlyUpdated()).hasSize(1);
        assertThat(snapshot.getLatestLegalChanges()).hasSize(1);
        assertThat(snapshot.getRecommendedForYou()).isEmpty();
    }

    @Test
    @DisplayName("English locale uses English category labels")
    void getCategories_englishLocale() {
        AppPreferences prefs = new AppPreferences();
        prefs.setLanguage("en");
        AppPreferences.set(prefs);

        when(repository.findByCategory(any(KnowledgeCategory.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        List<KnowledgeCategoryDto> categories = knowledgeService.getCategories();

        assertThat(categories.stream().map(KnowledgeCategoryDto::getLabel))
                .anyMatch(label -> label.contains("Taxes") || label.contains("FOP"));
    }

    private KnowledgeArticle sampleArticle(String slug, KnowledgeCategory category) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(1L);
        article.setSlug(slug);
        article.setCategory(category);
        article.setTitleUk("Заголовок " + slug);
        article.setTitleEn("Title " + slug);
        article.setContentUk("Контент українською");
        article.setContentEn("English content");
        article.setSummaryUk("Короткий опис");
        article.setSummaryEn("Short summary");
        article.setTagsUk("податки,фоп");
        article.setTagsEn("taxes,fop");
        article.setViewCount(10);
        article.setPublishedAt(LocalDate.now());
        return article;
    }
}
