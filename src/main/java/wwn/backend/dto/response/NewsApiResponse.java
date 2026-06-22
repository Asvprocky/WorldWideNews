package wwn.backend.dto.response;

import lombok.Data;
import wwn.backend.dto.request.NewsApiArticleDTO;

import java.util.List;

@Data
public class NewsApiResponse {
    private List<NewsApiArticleDTO> articles;
}
