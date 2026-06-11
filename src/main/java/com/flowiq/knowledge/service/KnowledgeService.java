package com.flowiq.knowledge.service;

import com.flowiq.config.AppPreferences;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.knowledge.dto.*;
import com.flowiq.knowledge.entity.KnowledgeArticle;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import com.flowiq.knowledge.provider.DatabaseKnowledgeProvider;
import com.flowiq.knowledge.provider.KnowledgeProvider;
import com.flowiq.knowledge.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeArticleRepository repository;
    private final DatabaseKnowledgeProvider databaseKnowledgeProvider;

    @Autowired(required = false)
    private List<KnowledgeProvider> knowledgeProviders;

    private static final Map<KnowledgeCategory, String> CATEGORY_LABELS_UK = Map.ofEntries(
            Map.entry(KnowledgeCategory.FOP_GROUPS, "Групи ФОП"),
            Map.entry(KnowledgeCategory.TAXES, "Податки"),
            Map.entry(KnowledgeCategory.ESV, "ЄСВ"),
            Map.entry(KnowledgeCategory.MILITARY_TAX, "Військовий збір"),
            Map.entry(KnowledgeCategory.DECLARATIONS, "Декларації"),
            Map.entry(KnowledgeCategory.KVED_DIRECTORY, "Довідник КВЕД"),
            Map.entry(KnowledgeCategory.ACCOUNTING_BASICS, "Основи обліку"),
            Map.entry(KnowledgeCategory.BUSINESS_FAQ, "Бізнес FAQ"),
            Map.entry(KnowledgeCategory.LEGAL_CHANGES, "Законодавчі зміни")
    );

    private static final Map<KnowledgeCategory, String> CATEGORY_LABELS_EN = Map.ofEntries(
            Map.entry(KnowledgeCategory.FOP_GROUPS, "FOP Groups"),
            Map.entry(KnowledgeCategory.TAXES, "Taxes"),
            Map.entry(KnowledgeCategory.ESV, "ESV"),
            Map.entry(KnowledgeCategory.MILITARY_TAX, "Military Tax"),
            Map.entry(KnowledgeCategory.DECLARATIONS, "Declarations"),
            Map.entry(KnowledgeCategory.KVED_DIRECTORY, "KVED Directory"),
            Map.entry(KnowledgeCategory.ACCOUNTING_BASICS, "Accounting Basics"),
            Map.entry(KnowledgeCategory.BUSINESS_FAQ, "Business FAQ"),
            Map.entry(KnowledgeCategory.LEGAL_CHANGES, "Legal Changes")
    );

    public KnowledgeArticlePageDto getArticles(int page, int size, KnowledgeCategory category, String tag) {
        boolean ukrainian = isUkrainian();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<KnowledgeArticle> articles = category == null
                ? repository.findAll(pageable)
                : repository.findByCategory(category, pageable);

        List<KnowledgeArticleSummaryDto> content = articles.getContent().stream()
                .filter(article -> matchesTag(article, tag, ukrainian))
                .map(article -> KnowledgeArticleSummaryDto.fromEntity(article, ukrainian))
                .toList();

        return KnowledgeArticlePageDto.builder()
                .content(content)
                .page(articles.getNumber())
                .size(articles.getSize())
                .totalElements(articles.getTotalElements())
                .totalPages(articles.getTotalPages())
                .build();
    }

    @Transactional
    public KnowledgeArticleDetailDto getArticleBySlug(String slug) {
        boolean ukrainian = isUkrainian();
        KnowledgeArticle article = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + slug));

        article.setViewCount(article.getViewCount() + 1);
        repository.save(article);

        List<KnowledgeArticleSummaryDto> related = findRelatedArticles(article, ukrainian);
        return KnowledgeArticleDetailDto.fromEntity(article, ukrainian, related);
    }

    public List<KnowledgeCategoryDto> getCategories() {
        boolean ukrainian = isUkrainian();
        return Arrays.stream(KnowledgeCategory.values())
                .map(category -> KnowledgeCategoryDto.builder()
                        .id(category)
                        .label(ukrainian ? CATEGORY_LABELS_UK.get(category) : CATEGORY_LABELS_EN.get(category))
                        .articleCount(repository.findByCategory(category, PageRequest.of(0, 1)).getTotalElements())
                        .build())
                .toList();
    }

    public KnowledgeSearchResponseDto search(String query, int page, int size, KnowledgeCategory category) {
        boolean ukrainian = isUkrainian();
        String normalized = normalizeQuery(query);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewCount", "updatedAt"));

        Page<KnowledgeArticle> articles = normalized.isBlank()
                ? (category == null ? repository.findAll(pageable) : repository.findByCategory(category, pageable))
                : (category == null
                    ? repository.search(normalized, pageable)
                    : repository.searchByCategory(normalized, category, pageable));

        List<KnowledgeArticleSummaryDto> results = articles.getContent().stream()
                .map(article -> KnowledgeArticleSummaryDto.fromEntityWithHighlight(article, ukrainian, normalized))
                .toList();

        KnowledgeProvider.KnowledgeAssistResult assist = resolveAssistResult(normalized, ukrainian, articles.getContent());

        return KnowledgeSearchResponseDto.builder()
                .query(query)
                .primaryArticle(assist.primaryArticle())
                .results(results)
                .relatedArticles(assist.relatedArticles())
                .quickSummary(assist.quickSummary())
                .page(articles.getNumber())
                .size(articles.getSize())
                .totalElements(articles.getTotalElements())
                .totalPages(articles.getTotalPages())
                .build();
    }

    public KnowledgeDashboardSnapshotDto getDashboardSnapshot() {
        boolean ukrainian = isUkrainian();
        List<KnowledgeArticleSummaryDto> popular = repository.findTop5ByOrderByViewCountDesc().stream()
                .map(article -> KnowledgeArticleSummaryDto.fromEntity(article, ukrainian))
                .toList();
        List<KnowledgeArticleSummaryDto> recent = repository.findTop5ByOrderByUpdatedAtDesc().stream()
                .map(article -> KnowledgeArticleSummaryDto.fromEntity(article, ukrainian))
                .toList();
        List<KnowledgeArticleSummaryDto> legal = repository
                .findByCategoryOrderByPublishedAtDesc(KnowledgeCategory.LEGAL_CHANGES).stream()
                .limit(5)
                .map(article -> KnowledgeArticleSummaryDto.fromEntity(article, ukrainian))
                .toList();
        List<KnowledgeArticleSummaryDto> recommended = buildRecommendations(ukrainian);

        return KnowledgeDashboardSnapshotDto.builder()
                .popularArticles(popular)
                .recentlyUpdated(recent)
                .recommendedForYou(recommended)
                .latestLegalChanges(legal)
                .build();
    }

    private List<KnowledgeArticleSummaryDto> buildRecommendations(boolean ukrainian) {
        List<String> recommendedSlugs = List.of(
                "fop-group-2-overview",
                "unified-tax-guide",
                "esv-payment-rules",
                "quarterly-declaration-fop"
        );
        return recommendedSlugs.stream()
                .map(repository::findBySlug)
                .flatMap(Optional::stream)
                .map(article -> KnowledgeArticleSummaryDto.fromEntity(article, ukrainian))
                .toList();
    }

    private List<KnowledgeArticleSummaryDto> findRelatedArticles(KnowledgeArticle article, boolean ukrainian) {
        String tagsRaw = ukrainian ? article.getTagsUk() : article.getTagsEn();
        if (tagsRaw == null || tagsRaw.isBlank()) {
            return repository.findByCategory(article.getCategory(), PageRequest.of(0, 4)).getContent().stream()
                    .filter(candidate -> !candidate.getSlug().equals(article.getSlug()))
                    .map(candidate -> KnowledgeArticleSummaryDto.fromEntity(candidate, ukrainian))
                    .toList();
        }
        String firstTag = tagsRaw.split(",")[0].trim();
        return repository.findRelatedByTag(
                        article.getCategory(),
                        article.getSlug(),
                        firstTag,
                        PageRequest.of(0, 4))
                .stream()
                .map(candidate -> KnowledgeArticleSummaryDto.fromEntity(candidate, ukrainian))
                .toList();
    }

    private KnowledgeProvider.KnowledgeAssistResult resolveAssistResult(
            String query,
            boolean ukrainian,
            List<KnowledgeArticle> candidates
    ) {
        KnowledgeArticle primary = candidates.isEmpty() ? null : candidates.get(0);
        KnowledgeProvider.KnowledgeSearchContext context = new KnowledgeProvider.KnowledgeSearchContext(
                query, ukrainian, candidates, primary
        );

        if (knowledgeProviders != null) {
            for (KnowledgeProvider provider : knowledgeProviders) {
                if (!(provider instanceof DatabaseKnowledgeProvider)) {
                    return provider.assistSearch(context);
                }
            }
        }
        return databaseKnowledgeProvider.assistSearch(context);
    }

    private boolean matchesTag(KnowledgeArticle article, String tag, boolean ukrainian) {
        if (tag == null || tag.isBlank()) {
            return true;
        }
        String tagsRaw = ukrainian ? article.getTagsUk() : article.getTagsEn();
        return tagsRaw != null && tagsRaw.toLowerCase(Locale.ROOT).contains(tag.toLowerCase(Locale.ROOT));
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private boolean isUkrainian() {
        return AppPreferences.current().isUkrainian();
    }
}
