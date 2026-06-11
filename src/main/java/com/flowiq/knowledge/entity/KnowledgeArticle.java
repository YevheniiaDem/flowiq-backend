package com.flowiq.knowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "title_uk", nullable = false, length = 255)
    private String titleUk;

    @Column(name = "title_en", nullable = false, length = 255)
    private String titleEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeCategory category;

    @Column(name = "content_uk", nullable = false, columnDefinition = "TEXT")
    private String contentUk;

    @Column(name = "content_en", nullable = false, columnDefinition = "TEXT")
    private String contentEn;

    @Column(name = "summary_uk", length = 500)
    private String summaryUk;

    @Column(name = "summary_en", length = 500)
    private String summaryEn;

    @Column(name = "impact_uk", length = 500)
    private String impactUk;

    @Column(name = "impact_en", length = 500)
    private String impactEn;

    @Column(name = "tags_uk", length = 500)
    private String tagsUk;

    @Column(name = "tags_en", length = 500)
    private String tagsEn;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
