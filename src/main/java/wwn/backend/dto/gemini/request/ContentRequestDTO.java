package wwn.backend.dto.gemini.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequestDTO {
    private List<PartRequestDTO> parts;
}
