package wwn.backend.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import wwn.backend.config.NewsClassifier;
import wwn.backend.domain.Article;
import wwn.backend.domain.NewsCategory;
import wwn.backend.domain.NewsSource;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.NewsSourceRepository;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssCollectorService {

    private final NewsSourceRepository newsSourceRepository;
    private final ArticleRepository articleRepository;
    private final NewsClassifier classifier; // 주입

    public void collect() {

        // 엑티브 된 기사 뉴스소스들만 가져
        List<NewsSource> sources = newsSourceRepository.findByIsActiveTrue();

        for (NewsSource source : sources) {
            try {
                URL url = new URL(source.getRssUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
                connection.setRequestProperty("Accept", "application/xml, text/xml, */*");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(connection));

                log.info("===== {} 수집 시작 =====", source.getName());

                for (SyndEntry entry : feed.getEntries()) {
                    // 1. [검증] 구글 뉴스 메인 페이지 설명글은 수집하지 않음
                    if (entry.getTitle().contains("Comprehensive up-to-date news coverage")) {
                        continue;
                    }

                    String articleUrl = entry.getLink();

                    // 1. [정제] 구글 뉴스 리다이렉트 주소라면 원본 주소로 복원
                    if (articleUrl.contains("news.google.com")) {
                        try {
                            // 접속하여 리다이렉트되는 최종 목적지(CNN 기사 페이지)를 가져옴
                            articleUrl = Jsoup.connect(articleUrl)
                                    .userAgent("Mozilla/5.0")
                                    .followRedirects(true)
                                    .execute()
                                    .url()
                                    .toString();
                        } catch (Exception e) {
                            log.warn("원본 URL 복원 실패, 구글 링크 유지: {}", articleUrl);
                        }
                    }

                    // 2. [정제] 제목에서 " - 언론사명" 제거 (예: "기사 제목 - CNN" -> "기사 제목")
                    String originalTitle = entry.getTitle();
                    if (originalTitle.contains(" - ")) {
                        originalTitle = originalTitle.split(" - ")[0];
                    }

                    if (articleRepository.existsByArticleUrl(articleUrl)) continue;

                    // 1. RSS 내장 카테고리 태그 우선 탐색
                    NewsCategory category = extractCategoryFromRss(entry);

                    // 2. 태그가 없거나 UNKNOWN이면 키워드 기반 분류기(classifier) 실행
                    if (category == NewsCategory.UNKNOWN) {
                        category = classifier.classify(entry.getTitle(), "");
                    }

                    String summary = fetchArticleSummary(articleUrl); // 이제 원본 CNN 페이지 접속
                    LocalDateTime publishedAt = (entry.getPublishedDate() != null)
                            ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : LocalDateTime.now();

                    Article article = Article.builder()
                            .country(source.getCountry())
                            .newsSource(source)
                            .originalTitle(originalTitle)
                            .translatedTitle(originalTitle)
                            .originalContent(summary)
                            .articleUrl(articleUrl)
                            .publishedAt(publishedAt)
                            .category(category)
                            .processed(false)
                            .build();

                    articleRepository.save(article);
                    log.info("저장 완료 : {} (요약글 길이: {})", entry.getTitle(), summary.length());

                    // [중요] 예의상 1초 대기 (사이트 차단 방지 및 크롤링 상대 서버 부하 방지)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                log.error("RSS 수집 실패 : {}", source.getName(), e);
            }
        }
    }

    // RSS 피드에서 카테고리 이름을 뽑아 Enum으로 변환하는 헬퍼 메서드
    private NewsCategory extractCategoryFromRss(SyndEntry entry) {
        if (entry.getCategories() == null || entry.getCategories().isEmpty()) {
            return NewsCategory.UNKNOWN;
        }

        // 첫 번째 카테고리 태그 사용 (RSS 제공자에 따라 다를 수 있음)
        String tag = entry.getCategories().get(0).getName().toUpperCase();

        if (tag.contains("POLITICS") || tag.contains("정치")) return NewsCategory.POLITICS;
        if (tag.contains("ECONOMY") || tag.contains("경제") || tag.contains("BUSINESS")) return NewsCategory.ECONOMY;
        if (tag.contains("WAR") || tag.contains("CONFLICT")) return NewsCategory.WAR;
        if (tag.contains("ACCIDENT") || tag.contains("사고")) return NewsCategory.ACCIDENT;

        return NewsCategory.UNKNOWN;
    }

    private String fetchArticleSummary(String url) {
        try {
            Document doc = Jsoup.connect(url).timeout(5000).get();

            // 1. 우선순위: og:description (페이스북/카톡 공유용 요약)
            String summary = doc.select("meta[property=og:description]").attr("content");

            // 2. 만약 없다면, 일반적인 description 메타태그라도 시도
            if (summary.isEmpty()) {
                summary = doc.select("meta[name=description]").attr("content");
            }

            return summary;

        } catch (Exception e) {
            log.warn("요약 추출 실패 (URL: {}) : {}", url, e.getMessage());
            return "";
        }
    }
}
