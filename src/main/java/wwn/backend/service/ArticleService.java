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
                        article.getCountry().getCountryNameKo(),
                        article.getNewsSource().getName(),
                        article.getArticleUrl(),
                        article.getPublishedAt(),
                        article.getCountry().getLatitude(),
                        article.getCountry().getLongitude()
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
                        article.getCountry().getCountryNameKo(),
                        article.getNewsSource().getName(),
                        article.getArticleUrl(),
                        article.getPublishedAt(),
                        article.getCountry().getLatitude(),
                        article.getCountry().getLongitude()
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
    public List<ArticleResponse> getByCountry(String name) { // name = "대한민국" 등

        // 1. 이름(countryNameKo)으로 Country 엔티티를 찾습니다.
        Country country = countryRepository.findByCountryNameKo(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 국가입니다: " + name));

        // 2. 찾은 country의 countryCode로 기사를 조회합니다.
        return articleRepository.findByCountry_CountryCodeOrderByPublishedAtDesc(country.getCountryCode())
                .stream()
                .map(article -> new ArticleResponse(
                        article.getId(),
                        article.getOriginalTitle(),
                        article.getCountry().getCountryNameKo(),
                        article.getNewsSource().getName(),
                        article.getArticleUrl(),
                        article.getPublishedAt(),
                        article.getCountry().getLatitude(),
                        article.getCountry().getLongitude()
                ))
                .toList();
    }
}
