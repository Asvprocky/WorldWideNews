package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.NewsSource;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
}
