package wwn.backend.dto.response;

import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        String title,
        String originalContent,
        String country,
        String source,
        String url,
        LocalDateTime publishedAt,
        Double lat,
        Double lng,
        String category
) {
}
