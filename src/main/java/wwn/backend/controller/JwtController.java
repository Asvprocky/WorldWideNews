package wwn.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wwn.backend.dto.request.JwtRefreshRequestDTO;
import wwn.backend.dto.response.JwtResponseDTO;
import wwn.backend.service.JwtService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jwt")
public class JwtController {

    private final JwtService jwtService;

    /**
     * 소셜 로그인 쿠키 방식의 Refresh 토큰 헤더 방식으로 교환
     */
    @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JwtResponseDTO jwtExchangeApi(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return jwtService.cookie2Header(request, response);
    }

    /**
     * Refresh 토큰으로 Access 토큰 재발급 (Rotate 포함)
     */
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JwtResponseDTO jwtRefreshApi(
            @Validated @RequestBody JwtRefreshRequestDTO dto
    ) {
        return jwtService.refreshRotate(dto);
    }

}
