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
import wwn.backend.dto.response.ArticleResponse;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.BookmarkRepository;
import wwn.backend.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    /**
     * 북마크 추가 & 취소
     *
     * @param articleId
     * @return
     */
    @Transactional
    public String toggleBookmark(Long articleId) {
        User user = getCurrentUser();

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기사입니다"));

        return bookmarkRepository.findByUserAndArticle(user, article)
                .map(bookmark -> {
                    bookmarkRepository.delete(bookmark);
                    return "북마크 취소 완료";
                })
                .orElseGet(() -> {
                    bookmarkRepository.save(Bookmark.builder().user(user).article(article).build());
                    return "북마크 추가 완료";
                });
    }

    /**
     * 로그인한 사용자 북마크 목록 추출
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<ArticleResponse> getBookmarkedArticles() {
        User user = getCurrentUser();

        return bookmarkRepository.findByUser(user)
                .stream()
                .map(bookmark -> convertToResponse(bookmark.getArticle(), user))
                .toList();
    }

    public ArticleResponse convertToResponse(Article article, User user) {
        boolean isBookmarked = (user != null) && bookmarkRepository.existsByUserAndArticle(user, article);

        return new ArticleResponse(
                article.getId(),
                article.getOriginalTitle(),
                article.getOriginalContent(),
                article.getThumbnailUrl(),
                article.getCountry().getCountryNameKo(),
                article.getNewsSource().getName(),
                article.getArticleUrl(),
                article.getPublishedAt(),
                article.getCountry().getLatitude(),
                article.getCountry().getLongitude(),
                article.getCategory().name(),
                isBookmarked
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (email == null || email.equals("anonymousUser")) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("유저 정보를 찾을 수 없습니다."));
    }
}