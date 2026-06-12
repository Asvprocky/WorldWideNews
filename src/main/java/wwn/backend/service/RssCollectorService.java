package wwn.backend.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwn.backend.domain.Article;
import wwn.backend.domain.NewsCategory;
import wwn.backend.domain.NewsSource;
import wwn.backend.repository.ArticleRepository;
import wwn.backend.repository.NewsSourceRepository;

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

    public void collect() {

        List<NewsSource> sources =
                newsSourceRepository.findByIsActiveTrue();

        for (NewsSource source : sources) {

            try {

                URL feedUrl = new URL(source.getRssUrl());

                SyndFeedInput input = new SyndFeedInput();

                SyndFeed feed =
                        input.build(new XmlReader(feedUrl));

                log.info("===== {} =====", source.getName());

                for (SyndEntry entry : feed.getEntries()) {

                    String articleUrl = entry.getLink();

                    // 중복 기사 스킵
                    if (articleRepository.existsByArticleUrl(articleUrl)) {
                        continue;
                    }

                    LocalDateTime publishedAt = null;

                    if (entry.getPublishedDate() != null) {
                        publishedAt = entry.getPublishedDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                    }

                    // 아티클 엔티티에 빌더 사용해도됨
                    Article article = Article.builder()
                            .country(source.getCountry())
                            .newsSource(source)
                            .originalTitle(entry.getTitle())
                            .translatedTitle(entry.getTitle()) // 나중에 GPT 번역
                            .originalContent("") // 임시
                            .articleUrl(articleUrl)
                            .publishedAt(publishedAt)
                            .category(NewsCategory.POLITICS) // 임시값, 나중에 GTP 로 분류
                            .processed(false)
                            .build();

                    articleRepository.save(article);

                    log.info("저장 완료 : {}", entry.getTitle());
                }

            } catch (Exception e) {

                log.error("RSS 수집 실패 : {}",
                        source.getName(), e);
            }
        }
    }
}