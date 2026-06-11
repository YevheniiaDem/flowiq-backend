package com.flowiq.knowledge.repository;

import com.flowiq.knowledge.entity.KnowledgeArticle;
import com.flowiq.knowledge.entity.KnowledgeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    Optional<KnowledgeArticle> findBySlug(String slug);

    Page<KnowledgeArticle> findByCategory(KnowledgeCategory category, Pageable pageable);

    @Query("""
            SELECT a FROM KnowledgeArticle a
            WHERE LOWER(a.titleUk) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.titleEn) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.contentUk) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.contentEn) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.tagsUk) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.tagsEn) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.summaryUk) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(a.summaryEn) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<KnowledgeArticle> search(@Param("query") String query, Pageable pageable);

    @Query("""
            SELECT a FROM KnowledgeArticle a
            WHERE (:category IS NULL OR a.category = :category)
              AND (
                    LOWER(a.titleUk) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(a.titleEn) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(a.contentUk) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(a.contentEn) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(a.tagsUk) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(a.tagsEn) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            """)
    Page<KnowledgeArticle> searchByCategory(
            @Param("query") String query,
            @Param("category") KnowledgeCategory category,
            Pageable pageable
    );

    List<KnowledgeArticle> findTop5ByOrderByViewCountDesc();

    List<KnowledgeArticle> findTop5ByOrderByUpdatedAtDesc();

    List<KnowledgeArticle> findByCategoryOrderByPublishedAtDesc(KnowledgeCategory category);

    @Query("""
            SELECT a FROM KnowledgeArticle a
            WHERE a.category = :category
              AND a.slug <> :slug
              AND (
                    a.tagsUk LIKE CONCAT('%', :tag, '%')
                 OR a.tagsEn LIKE CONCAT('%', :tag, '%')
              )
            ORDER BY a.viewCount DESC
            """)
    List<KnowledgeArticle> findRelatedByTag(
            @Param("category") KnowledgeCategory category,
            @Param("slug") String slug,
            @Param("tag") String tag,
            Pageable pageable
    );
}
