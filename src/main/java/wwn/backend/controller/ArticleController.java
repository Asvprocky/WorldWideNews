package wwn.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wwn.backend.dto.response.ArticleResponse;
import wwn.backend.service.ArticleService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;


    /**
     * 모든 기사 최신순으로 50개 만 호출
     *
     * @return
     */
    @GetMapping
    List<ArticleResponse> getArticles() {
        return articleService.getArticles();
    }

    /**
     * 모든 기사 최신순서로 호출
     *
     * @return
     */
    @GetMapping("/all")
    public List<ArticleResponse> getAllArticles() {
        return articleService.getAllArticles();
    }


    /**
     * 나라별로 모든기사 최신순으로 호출
     *
     * @param name
     * @return
     */
    @GetMapping("/country/{name}")
    public List<ArticleResponse> getByCountry(
            @PathVariable String name
    ) {
        System.out.println("요청받은 국가 코드: [" + name + "]"); // 공백이 있는지 확인!
        List<ArticleResponse> list = articleService.getByCountry(name);
        System.out.println("조회된 기사 개수: " + list.size());
        return list;
    }

}
