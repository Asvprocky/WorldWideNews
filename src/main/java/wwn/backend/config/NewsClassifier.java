package wwn.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import wwn.backend.domain.NewsCategory;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NewsClassifier {

    public NewsCategory classify(String title, String content) {
        String text = (title + " " + content).toLowerCase();

        Map<NewsCategory, Integer> scores = new HashMap<>();
        scores.put(NewsCategory.WAR, 0);
        scores.put(NewsCategory.ECONOMY, 0);
        scores.put(NewsCategory.ACCIDENT, 0);

        // 카테고리별 키워드 및 가중치 설정
        addScore(scores, text, NewsCategory.WAR, new String[]{"전쟁", "군사", "미사일", "공격", "충돌", "군대", "우크라이나"});
        addScore(scores, text, NewsCategory.ECONOMY, new String[]{"금리", "환율", "유가", "주가", "인플레이션", "경제", "수출", "물가", "달러"});
        addScore(scores, text, NewsCategory.ACCIDENT, new String[]{"사고", "화재", "사망", "범죄", "살인", "재난", "실종", "추락"});

        // 가장 높은 점수를 받은 카테고리 반환
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(NewsCategory.UNKNOWN); // 기본값
    }

    private void addScore(Map<NewsCategory, Integer> scores, String text, NewsCategory cat, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                scores.put(cat, scores.get(cat) + 1);
            }
        }
    }
}
