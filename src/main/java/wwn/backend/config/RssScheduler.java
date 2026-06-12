package wwn.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wwn.backend.service.RssCollectorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RssScheduler {
    private final RssCollectorService rssCollectorService;

    @Scheduled(fixedRate = 30000)

    public void collectNews() {

        log.info("RSS 수집 시작");

        rssCollectorService.collect();

        log.info("RSS 수집 종료");

    }

}
