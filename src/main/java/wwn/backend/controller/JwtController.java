package wwn.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wwn.backend.dto.response.JwtResponseDTO;
import wwn.backend.service.JwtService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jwt")
public class JwtController {

    private final JwtService jwtService;

    /**
     * 일반 로그인 토큰 재발급 및 소셜 로그인 토큰 교환 통합
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDTO> jwtRefreshApi(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        JwtResponseDTO result = jwtService.refresh(request, response);
        return ResponseEntity.ok(result);
    }
}
