package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import wwn.backend.domain.Article;
import wwn.backend.domain.NewsCategory;
import wwn.backend.domain.NewsSource;
import wwn.backend.dto.request.NewsApiArticleDTO;
import wwn.backend.dto.response.NewsApiResponse;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.NewsSourceRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsApiCollectorService {

    private final ArticleRepository articleRepository;
    private final NewsSourceRepository newsSourceRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${news.api.key}")
    private String newsApiKey;

    public void collectGlobalNews() {
        String[] countries = {"us"};

        for (String countryCode : countries) {
            try {
                log.info("===== NewsAPI {} 수집 시작 =====", countryCode);

                NewsSource source = newsSourceRepository.findFirstByCountry_CountryCodeIgnoreCase(countryCode)
                        .orElse(null);

                if (source == null) {
                    log.warn("{} 에 해당하는 NewsSource가 DB에 없습니다. 패스합니다.", countryCode);
                    continue;
                }

                String apiUrl = "https://newsapi.org/v2/top-headlines?country=" + countryCode + "&apiKey=" + newsApiKey;

                // newsAPI 에서 내려받은 JSON 형식을 NewsApiResponse 객체로 자동 변환
                NewsApiResponse response = restTemplate.getForObject(apiUrl, NewsApiResponse.class);


                if (response != null && response.getArticles() != null) {

                    int successCount = 0;
                    int duplicateCount = 0;
                    int nullUrlCount = 0;

                    for (NewsApiArticleDTO dto : response.getArticles()) {

                        log.info("[디버깅] 현재 기사의 URL 값: '{}'", dto.getUrl());

                        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
                            nullUrlCount++;
                            continue;
                        }

                        // [수정됨] existsBy 대신 countBy를 사용하여 DB에 저장된 개수를 가져옴
                        long urlCount = articleRepository.countByArticleUrl(dto.getUrl());

                        // DB에 해당 URL이 1개 이상 존재하면 진짜 중복이므로 패스
                        if (urlCount > 0) {
                            duplicateCount++;
                            continue;
                        }

                        // DTO -> Entity
                        try {
                            Article article = Article.builder()
                                    .newsSource(source)
                                    .country(source.getCountry())
                                    .originalTitle(dto.getTitle())
                                    .translatedTitle(dto.getTitle())
                                    .originalContent(dto.getDescription() != null ? dto.getDescription() : "")
                                    .thumbnailUrl(dto.getUrlToImage())
                                    .articleUrl(dto.getUrl())
                                    .publishedAt(parseDate(dto.getPublishedAt()))
                                    .category(NewsCategory.UNKNOWN)
                                    .processed(false)
                                    .build();

                            articleRepository.save(article);
                            successCount++;

                        } catch (Exception e) {
                            log.error("❌ [디버깅] 개별 기사 DB 저장 중 에러 발생 제목: {}, 사유: {}", dto.getTitle(), e.getMessage());
                        }
                    }

                    log.info("[최종 결과 수집 리포트] 성공: {}개, 중복 패스: {}개, URL 누락: {}개",
                            successCount, duplicateCount, nullUrlCount);
                }

            } catch (Exception e) {
                log.error("NewsAPI 수집 실패: {}", countryCode, e);
            }
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
    }
}