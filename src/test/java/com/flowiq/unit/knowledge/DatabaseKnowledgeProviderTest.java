package com.flowiq.unit.knowledge;

import com.flowiq.knowledge.entity.KnowledgeArticle;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import com.flowiq.knowledge.provider.DatabaseKnowledgeProvider;
import com.flowiq.knowledge.provider.KnowledgeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DatabaseKnowledgeProvider unit tests")
class DatabaseKnowledgeProviderTest {

    private DatabaseKnowledgeProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DatabaseKnowledgeProvider();
    }

    @Test
    @DisplayName("assistSearch returns not-found message when no candidates")
    void assistSearch_noResults() {
        var context = new KnowledgeProvider.KnowledgeSearchContext("tax", false, List.of(), null);

        var result = provider.assistSearch(context);

        assertThat(result.quickSummary()).contains("No articles found");
        assertThat(result.primaryArticle()).isNull();
    }

    @Test
    @DisplayName("assistSearch ranks and summarizes matching articles")
    void assistSearch_withCandidates() {
        KnowledgeArticle primary = article("fop-tax", "FOP Tax Guide", "Podatky FOP", "tax fop");
        KnowledgeArticle related = article("esv", "ESV Payments", "ЄСВ", "tax esv");

        var context = new KnowledgeProvider.KnowledgeSearchContext(
                "fop tax", false, List.of(related, primary), null);

        var result = provider.assistSearch(context);

        assertThat(result.primaryArticle()).isNotNull();
        assertThat(result.primaryArticle().getSlug()).isEqualTo("fop-tax");
        assertThat(result.quickSummary()).isNotBlank();
        assertThat(result.relatedArticles()).isNotEmpty();
    }

    @Test
    @DisplayName("assistSearch uses Ukrainian messages when locale is Ukrainian")
    void assistSearch_ukrainian() {
        var context = new KnowledgeProvider.KnowledgeSearchContext("податок", true, List.of(), null);

        var result = provider.assistSearch(context);

        assertThat(result.quickSummary()).contains("Статей");
    }

    @Test
    @DisplayName("assistSearch uses provided primary article when set")
    void assistSearch_primaryProvided() {
        KnowledgeArticle primary = article("primary", "Primary", "Primary UK", "content");
        var context = new KnowledgeProvider.KnowledgeSearchContext(
                "query", false, List.of(primary), primary);

        var result = provider.assistSearch(context);

        assertThat(result.primaryArticle().getSlug()).isEqualTo("primary");
    }

    private KnowledgeArticle article(String slug, String titleEn, String titleUk, String tags) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setSlug(slug);
        article.setTitleEn(titleEn);
        article.setTitleUk(titleUk);
        article.setCategory(KnowledgeCategory.TAXES);
        article.setContentEn("Content about " + tags);
        article.setContentUk("Контент про " + tags);
        article.setSummaryEn("Summary " + tags);
        article.setSummaryUk("Підсумок " + tags);
        article.setTagsEn(tags);
        article.setTagsUk(tags);
        article.setViewCount(10);
        return article;
    }
}
