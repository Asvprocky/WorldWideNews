package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wwn.backend.domain.Article;
import wwn.backend.domain.Bookmark;
import wwn.backend.domain.User;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 특정 유저가 특정 기사를 이미 북마크했는지 확인할 때 사용
    boolean existsByUserAndArticle(User user, Article article);

    // 북마크를 취소(삭제)할 때 사용
    Optional<Bookmark> findByUserAndArticle(User user, Article article);


    // 유저의 북마크 목록을 가져오면서, 관련 기사 + 국가 + 뉴스소스까지 한 번에 DB에서 로딩 FETCH JOIN
    @Query("SELECT b FROM Bookmark b JOIN FETCH b.article a " +
            "LEFT JOIN FETCH a.country " +
            "LEFT JOIN FETCH a.newsSource " +
            "WHERE b.user = :user")
    List<Bookmark> findByUser(@Param("user") User user);
}

