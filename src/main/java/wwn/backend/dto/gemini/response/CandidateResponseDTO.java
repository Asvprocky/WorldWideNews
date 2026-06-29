package wwn.backend.dto.gemini.response;

public record CandidateResponseDTO(ContentResponseDTO content,
                                   String finishReason) {
}
