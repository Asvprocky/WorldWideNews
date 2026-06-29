package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import wwn.backend.dto.gemini.request.ContentRequestDTO;
import wwn.backend.dto.gemini.request.GeminiRequestDTO;
import wwn.backend.dto.gemini.request.PartRequestDTO;
import wwn.backend.dto.gemini.response.GeminiResponseDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final RestClient restClient;
    @Value("${gemini.api-key}")
    private String apiKey;

    public String test() {

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        GeminiRequestDTO request = new GeminiRequestDTO(
                List.of(
                        new ContentRequestDTO(
                                List.of(
                                        new PartRequestDTO("안녕하세요. 한 줄만 인사해주세요.")
                                )
                        )
                )
        );

        GeminiResponseDTO response = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<GeminiResponseDTO>() {
                });

        if (response == null) {

            throw new RuntimeException("Gemini API 응답이 없습니다.");

        }

        return response.getText();
    }


}
