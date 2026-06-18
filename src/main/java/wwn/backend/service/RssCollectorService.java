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
    private final NewsClassifier classifier;

    public void collect() {
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
                    String originalTitle = entry.getTitle();

                    // [CNN 정제]
                    if ("CNN".equalsIgnoreCase(source.getName())) {
                        String desc = (entry.getDescription() != null) ? entry.getDescription().getValue() : "";
                        if (desc != null && !desc.isEmpty()) {
                            String extracted = Jsoup.parse(desc).text();
                            if (extracted != null && extracted.length() > 5 && !extracted.trim().equals("- CNN")) {
                                originalTitle = extracted.replace("CNN", "").trim();
                            }
                        }
                    }

                    if (originalTitle != null && originalTitle.contains(" - ")) {
                        originalTitle = originalTitle.split(" - ")[0];
                    }

                    if (originalTitle == null || originalTitle.trim().length() < 5) {
                        log.info("무효한 기사 제목 감지됨: {}", originalTitle);
                        continue;
                    }

                    String articleUrl = entry.getLink();
                    if (articleUrl != null && articleUrl.contains("news.google.com")) {
                        try {
                            org.jsoup.Connection.Response response = Jsoup.connect(articleUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                                    .followRedirects(true).timeout(5000).execute();
                            String finalUrl = response.url().toString();
                            if (finalUrl != null && !finalUrl.contains("news.google.com")) {
                                articleUrl = finalUrl;
                            }
                        } catch (Exception e) {
                            log.warn("URL 복원 실패: {}", articleUrl);
                        }
                    }

                    if (articleRepository.existsByArticleUrl(articleUrl)) continue;

                    // [데이터 추출: 요약 & 썸네일 통합]
                    String summary = "";
                    String thumbnailUrl = "";
                    try {
                        Document doc = Jsoup.connect(articleUrl)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                                .timeout(5000).get();

                        System.out.println(doc.location());
                        System.out.println(doc.title());

                        summary = doc.select("meta[property=og:description]").attr("content");
                        if (summary.isEmpty()) summary = doc.select("meta[name=description]").attr("content");

                        thumbnailUrl = fetchThumbnailUrl(doc);

                        log.info("기사 URL = {}", articleUrl);
                        log.info("썸네일 = {}", thumbnailUrl);

                    } catch (Exception e) {
                        log.warn("본문 데이터 추출 실패: {}", articleUrl);
                    }

                    NewsCategory category = extractCategoryFromRss(entry);
                    if (category == NewsCategory.UNKNOWN) {
                        category = classifier.classify(originalTitle, "");
                    }

                    LocalDateTime publishedAt = (entry.getPublishedDate() != null)
                            ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : LocalDateTime.now();

                    Article article = Article.builder()
                            .country(source.getCountry())
                            .newsSource(source)
                            .originalTitle(originalTitle)
                            .translatedTitle(originalTitle)
                            .originalContent(summary)
                            .thumbnailUrl(thumbnailUrl) // 썸네일 저장
                            .articleUrl(articleUrl)
                            .publishedAt(publishedAt)
                            .category(category)
                            .processed(false)
                            .build();

                    articleRepository.save(article);
                    log.info("저장 완료: {} (이미지: {})", originalTitle, thumbnailUrl.isEmpty() ? "없음" : "있음");

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("RSS 수집 실패 : {}", source.getName(), e);
            }
        }
    }

    private String fetchThumbnailUrl(Document doc) {
        String imageUrl = doc.select("meta[property=og:image]").attr("content");
        if (imageUrl.isEmpty()) {
            imageUrl = doc.select("meta[name=twitter:image]").attr("content");
        }
        return imageUrl;
    }

    private NewsCategory extractCategoryFromRss(SyndEntry entry) {
        if (entry.getCategories() == null || entry.getCategories().isEmpty()) return NewsCategory.UNKNOWN;
        String tag = entry.getCategories().get(0).getName().toUpperCase();
        if (tag.contains("POLITICS") || tag.contains("정치")) return NewsCategory.POLITICS;
        if (tag.contains("ECONOMY") || tag.contains("경제") || tag.contains("BUSINESS")) return NewsCategory.ECONOMY;
        if (tag.contains("WAR") || tag.contains("CONFLICT")) return NewsCategory.WAR;
        if (tag.contains("ACCIDENT") || tag.contains("사고")) return NewsCategory.ACCIDENT;
        return NewsCategory.UNKNOWN;
    }
}
