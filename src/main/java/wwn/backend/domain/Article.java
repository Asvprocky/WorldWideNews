package wwn.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 국가 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    // 뉴스 출처 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private NewsSource newsSource;

    @Column(name = "original_title", nullable = false)
    private String originalTitle;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "translated_title")
    private String translatedTitle;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "article_url", nullable = false, unique = true)
    private String articleUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NewsCategory category;

    @Column(nullable = false)
    private boolean processed = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Builder
    public Article(
            Country country,
            NewsSource newsSource,
            String originalTitle,
            String originalContent,
            String translatedTitle,
            String summary,
            String articleUrl,
            String thumbnailUrl,
            LocalDateTime publishedAt,
            NewsCategory category,
            boolean processed
    ) {

        this.country = country;
        this.newsSource = newsSource;
        this.originalTitle = originalTitle;
        this.originalContent = originalContent;
        this.translatedTitle = translatedTitle;
        this.summary = summary;
        this.articleUrl = articleUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
        this.category = category;
        this.processed = processed;
    }
}