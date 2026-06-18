package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.Country;
import wwn.backend.dto.response.ArticleResponse;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.CountryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public List<ArticleResponse> getAllArticles() {

        return articleRepository.findAllByOrderByPublishedAtDesc()
                .stream()
                .map(article -> new ArticleResponse(
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
                        article.getCategory().name()
                ))
                .toList();
    }


    /**
     * 최신 기사 50 개 출력
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<ArticleResponse> getArticles() {

        return articleRepository.findTop50ByOrderByPublishedAtDesc()
                .stream()
                .map(article -> new ArticleResponse(
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
                        article.getCategory().name()
                ))
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
        // 기존 로직을 그대로 사용하되, 검색 범위를 넓혀 에러를 방지합니다.
        Country country = countryRepository.findByCountryNameKo(name)
                .or(() -> countryRepository.findByCountryCode(name)) // 추가: 코드로도 검색
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 국가입니다: " + name));

        return articleRepository.findByCountry_CountryCodeOrderByPublishedAtDesc(country.getCountryCode())
                .stream()
                .map(article -> new ArticleResponse(
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
                        article.getCategory().name()
                ))
                .toList();
    }
}
