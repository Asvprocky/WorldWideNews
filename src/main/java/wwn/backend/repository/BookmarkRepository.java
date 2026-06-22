package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.Article;
import wwn.backend.domain.Bookmark;
import wwn.backend.domain.User;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 특정 유저가 특정 기사를 이미 북마크했는지 확인할 때 사용
    boolean existsByUserAndArticle(User user, Article article);

    // 북마크를 취소(삭제)할 때 사용
    Optional<Bookmark> findByUserAndArticle(User user, Article article);
}

