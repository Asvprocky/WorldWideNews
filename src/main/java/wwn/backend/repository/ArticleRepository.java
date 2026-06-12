package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.Article;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByArticleUrl(String articleUrl);

    // 전체 뉴스 최신순
    List<Article> findAllByOrderByPublishedAtDesc();

    // 50 개만 최신순
    List<Article> findTop50ByOrderByPublishedAtDesc();

    // 나라별 뉴스 최신순
    List<Article> findByCountry_CountryCodeOrderByPublishedAtDesc(String countryCode);

}
