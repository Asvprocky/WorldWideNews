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

        // 엑티브 된 기사 뉴스소스들만 가져옴
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
                    // 1. [정제] 제목 추출 (CNN인 경우)
                    String originalTitle = entry.getTitle();

                    if ("CNN".equalsIgnoreCase(source.getName())) {
                        String desc = (entry.getDescription() != null) ? entry.getDescription().getValue() : "";

                        if (desc != null && !desc.isEmpty()) {
                            String extracted = Jsoup.parse(desc).text();
                            // " - CNN" 이라는 껍데기만 남은 경우, extracted는 " - CNN" 혹은 그냥 ""이 됨.
                            // 따라서 제목이 정상적으로 추출되었는지 확인.
                            if (extracted != null && extracted.length() > 5 && !extracted.trim().equals("- CNN")) {
                                originalTitle = extracted.replace("CNN", "").trim();
                            }
                        }
                    }

                    // [정제] " - 언론사명" 제거
                    if (originalTitle != null && originalTitle.contains(" - ")) {
                        originalTitle = originalTitle.split(" - ")[0];
                    }

                    // [핵심 정제] 여기서 확실하게 걸러냄.
                    // 1. null인가?
                    // 2. "CNN"이나 " - "만 남아있는가?
                    // 3. 길이가 5자 미만인가?
                    if (originalTitle == null ||
                            originalTitle.trim().length() < 5) {

                        log.info("무효한 기사 제목 감지됨, 건너뜀: {}", originalTitle);
                        continue;
                    }

                    // 2. [정제] URL 복원 구글 봇 감지 우회 추가 영역
                    String articleUrl = entry.getLink();
                    if (articleUrl != null && articleUrl.contains("news.google.com")) {
                        try {
                            // 단순 url() 확인이 아니라 브라우저와 완벽히 동일한 헤더 정보를 주어 봇 차단을 통과.
                            org.jsoup.Connection.Response response = Jsoup.connect(articleUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                                    .header("Cache-Control", "no-cache")
                                    .header("Pragma", "no-cache")
                                    .header("Sec-Ch-Ua", "\"Not A(Brand\";v=\"99\", \"Google Chrome\";v=\"121\", \"Chromium\";v=\"121\"")
                                    .header("Sec-Ch-Ua-Mobile", "?0")
                                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                                    .followRedirects(true)
                                    .timeout(5000)
                                    .execute();

                            String finalUrl = response.url().toString();

                            // 정상적으로 구글 밖의 언론사 주소로 탈출했을 때만 변환값을 적용.
                            if (finalUrl != null && !finalUrl.contains("news.google.com")) {
                                articleUrl = finalUrl;
                            }
                        } catch (Exception e) {
                            log.warn("원본 URL 복원 실패, 구글 링크 유지: {} (원인: {})", articleUrl, e.getMessage());
                        }
                    }

                    if (articleRepository.existsByArticleUrl(articleUrl)) continue;

                    // 3. 분류 및 요약
                    NewsCategory category = extractCategoryFromRss(entry);
                    if (category == NewsCategory.UNKNOWN) {
                        category = classifier.classify(originalTitle, "");
                    }

                    String summary = fetchArticleSummary(articleUrl);
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
                    log.info("저장 완료 : {} (요약글 길이: {})", originalTitle, summary.length());

                    Thread.sleep(1000);
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
            // 원문 사이트(CNN 본사 등)에서도 봇 감지가 일어날 수 있으므로 User-Agent 정보를 추가 제공.
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

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
