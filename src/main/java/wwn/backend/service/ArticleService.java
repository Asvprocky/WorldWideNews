package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.Article;
import wwn.backend.domain.Country;
import wwn.backend.domain.User;
import wwn.backend.dto.response.ArticleResponse;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.BookmarkRepository;
import wwn.backend.repository.CountryRepository;
import wwn.backend.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CountryRepository countryRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 모든 기사 출력
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<ArticleResponse> getAllArticles() {
        User user = getCurrentUser();

        return articleRepository.findAllByOrderByPublishedAtDesc()
                .stream()
                .map(article -> convertToResponse(article, user))
                .toList();
    }


    /**
     * 최신 기사 50 개 출력
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<ArticleResponse> getArticles() {
        User user = getCurrentUser();

        return articleRepository.findTop50ByOrderByPublishedAtDesc()
                .stream()
                .map(article -> convertToResponse(article, user))
                .toList();
    }

    /**
     * 나라별로 최신 기사 출력
     *
     * @param name
     * @return
     */
    @Transactional(readOnly = true)
    public List<ArticleResponse> getByCountry(String name) {
        User user = getCurrentUser();
        // 기존 로직을 그대로 사용하되, 검색 범위를 넓혀 에러를 방지
        Country country = countryRepository.findByCountryNameKo(name)
                .or(() -> countryRepository.findByCountryCode(name)) // 추가: 코드로도 검색
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 국가입니다: " + name));

        return articleRepository.findByCountry_CountryCodeOrderByPublishedAtDesc(country.getCountryCode())
                .stream()
                .map(article -> convertToResponse(article, user))
                .toList();
    }

    // 중복 해결: 변환 로직 전용 메서드
    private ArticleResponse convertToResponse(Article article, User user) {
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

    //  유저 가져오는 로직 분리
    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        System.out.println("auth = " + auth);
        System.out.println("name = " + auth.getName());

        if (email == null || email.equals("anonymousUser")) return null;
        User user = userRepository.findByEmail(email).orElse(null);
        System.out.println("user = " + user);
        return userRepository.findByEmail(email).orElse(null);
    }
}
