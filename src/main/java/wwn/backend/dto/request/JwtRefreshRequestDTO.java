package wwn.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtRefreshRequestDTO {
    @NotBlank
    private String refreshToken;
}
