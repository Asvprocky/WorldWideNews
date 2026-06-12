package wwn.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wwn.backend.service.RssCollectorService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final RssCollectorService rssCollectorService;

    @PostMapping("/rss")
    public String rss() {

        rssCollectorService.collect();

        return "success";

    }

}
