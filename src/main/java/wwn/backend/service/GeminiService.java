package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    public String test() {

        return "API KEY : " + apiKey.substring(0, 10) + "...";

    }

}
