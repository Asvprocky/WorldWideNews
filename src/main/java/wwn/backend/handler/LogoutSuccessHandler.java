package wwn.backend.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import wwn.backend.service.JwtService;
import wwn.backend.util.JWTUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class LogoutSuccessHandler implements LogoutHandler {

    private final JwtService jwtService;

    /**
     * 로그아웃 시 DB의 RefreshToken 제거.
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            // [최적화] 복잡한 스트림 변환 대신 스프링 표준 내장 도구(StreamUtils)로 간결하게 읽어옴
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            // 데이터 검증 수행
            if (!StringUtils.hasText(body)) return;

            // JSON 파싱 진행
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(body);

            // "refreshToken" 키 존재 여부 확인 후 값 추출함
            String refreshToken = jsonNode.has("refreshToken") ? jsonNode.get("refreshToken").asText() : null;

            // 토큰 null 체크 및 유효성 검증 (앞서 수정한 원시 타입 boolean isValid 활용)
            if (refreshToken == null || !JWTUtil.isValid(refreshToken, false)) {
                return; // 유효하지 않은 토큰은 무시하고 종료함
            }

            // DB에서 해당 리프레시 토큰 안전하게 영구 삭제함 (화이트리스트 파괴)
            jwtService.deleteRefresh(refreshToken);

        } catch (IOException e) {
            // 스트림 읽기 장애 시 예외 발생 처리함
            throw new RuntimeException("Failed to read refresh token", e);
        }
    }
}
