package wwn.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wwn.backend.service.NewsApiCollectorService;
import wwn.backend.service.RssCollectorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RssScheduler {
    private final RssCollectorService rssCollectorService;

    private final NewsApiCollectorService newsApiCollectorService;

    // 30 분마다 호출
    @Scheduled(fixedRate = 1000 * 60 * 30)

    public void collectNews() {

        log.info("한국 , 일본 RSS 수집 시작");

        rssCollectorService.collect();

        log.info("RSS 수집 종료");

        // 미국 등 해외

        log.info("미국 등 해외 RSS 수집 시작");

        newsApiCollectorService.collectGlobalNews();

        log.info("RSS 수집 종료");


    }

}
