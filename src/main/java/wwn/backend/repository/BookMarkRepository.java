package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.Bookmark;

public interface BookMarkRepository extends JpaRepository<Bookmark, Long> {
}
