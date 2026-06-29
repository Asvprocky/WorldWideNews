package wwn.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wwn.backend.service.GeminiService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiTestController {

    private final GeminiService geminiService;

    @GetMapping("/test")
    public String test() {
        return geminiService.test();
    }
}
