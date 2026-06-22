package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.Article;
import wwn.backend.domain.Bookmark;
import wwn.backend.domain.User;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.BookmarkRepository;
import wwn.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository; // User 엔티티용 리포지토리가 있다고 가정
    private final ArticleRepository articleRepository;

    @Transactional
    public String toggleBookmark(Long articleId) {
        // 1. 유저와 기사 존재 여부 확인
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기사입니다"));

        return bookmarkRepository.findByUserAndArticle(user, article)
                .map(bookmark -> {
                    bookmarkRepository.delete(bookmark);
                    return "북마크 취소 완료";
                })
                .orElseGet(() -> {
                    Bookmark bookmark = Bookmark.builder()
                            .user(user)
                            .article(article)
                            .build();
                    bookmarkRepository.save(bookmark);
                    return "북마크 추가 완료";
                });

    }

}
