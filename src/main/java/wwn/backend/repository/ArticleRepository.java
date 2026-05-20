package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
