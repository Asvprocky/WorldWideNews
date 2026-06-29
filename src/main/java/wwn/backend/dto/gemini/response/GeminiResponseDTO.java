package wwn.backend.dto.gemini.response;


import java.util.List;

public record GeminiResponseDTO(
        List<CandidateResponseDTO> candidates
) {

    public String getText() {
        return candidates.get(0)
                .content()
                .parts()
                .get(0)
                .text();
    }

}
