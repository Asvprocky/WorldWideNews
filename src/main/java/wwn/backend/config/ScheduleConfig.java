package wwn.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wwn.backend.repository.JwtRefreshRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScheduleConfig {


    private final JwtRefreshRepository jwtRefreshRepository;

    /**
     * Refresh 토큰 저장소 8일 지난 토큰 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void refreshEntityTtlSchedule() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
        jwtRefreshRepository.deleteByCreatedAtBefore(cutoff);
    }
}
