package wwn.backend.dto.request;

import lombok.Data;

@Data
public class NewsApiArticleDTO {
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private String publishedAt;
}
