package wwn.backend.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import wwn.backend.domain.JwtRefresh;
import wwn.backend.dto.response.JwtResponseDTO;
import wwn.backend.repository.JwtRefreshRepository;
import wwn.backend.util.JWTUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtRefreshRepository jwtRefreshRepository;

    /**
     * RefreshToken 발급 후 저장
     */
    @Transactional
    public void addRefresh(String email, String refreshToken) {
        JwtRefresh jwtRefreshEntity = JwtRefresh.builder()
                .email(email)
                .refresh(refreshToken)
                .build();

        jwtRefreshRepository.save(jwtRefreshEntity);
    }


    /**
     * RefreshToken 존재 유효성 체크
     */
    @Transactional(readOnly = true)
    public Boolean existsRefresh(String refreshToken) {
        return jwtRefreshRepository.existsByRefresh(refreshToken);

    }

    /**
     * 통합 완결, 쿠키 기반 AccessToken 재발급 & 소셜 로그인 토큰 교환 (Rotate 포함)
     */
    @Transactional
    public JwtResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {

        // 1. Request에서 쿠키 보따리 꺼내기
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "RefreshToken 쿠키 없음");
        }

        // 2. 통합 쿠키 이름인 "refresh_token" (또는 이전 프로젝트처럼 "refreshToken") 찾기
        // ⚠️ 프론트엔드(Next.js)와 이름을 완전히 통일해야 합니다. 여기선 "refresh_token"으로 세팅할게요.
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refreshToken 없음");
        }

        // 3. 토큰 유효성 검증
        if (!JWTUtil.isValid(refreshToken, false)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refreshToken 만료");
        }

        // 4. DB 화이트리스트 확인
        // 소셜 로그인 직후 최초 교환 시점에는 DB에 없을 수 있으므로 유연하게 체크하거나,
        // 소셜 핸들러에서 발급할 때 미리 DB에 인서트해 두면 이 조건문을 부드럽게 통과합니다.
        if (!existsRefresh(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refreshToken 위조 또는 누락");
        }

        // 5. 토큰 정보 추출 및 신규 토큰 세트 생성
        String email = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        String newAccessToken = JWTUtil.createJWT(email, role, true);
        String newRefreshToken = JWTUtil.createJWT(email, role, false);

        // 6. DB 갱신 (기존 토큰 무효화 후 새 토큰 저장)
        deleteRefresh(refreshToken);
        jwtRefreshRepository.flush(); // 순서 꼬임 방지

        JwtRefresh newRefreshEntity = JwtRefresh.builder()
                .email(email)
                .refresh(newRefreshToken)
                .build();
        jwtRefreshRepository.save(newRefreshEntity);

        // 7. 새롭게 Rotate된 리프레시 토큰을 다시 HttpOnly 쿠키로 밀어넣기
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax") // Next.js와 크로스 도메인 이슈가 있다면 "None"(Secure필수) 또는 "Lax" 사용
                .secure(false)   // 로컬(http) 환경에선 false, 나중에 AWS 배포(https)시 true로 변경
                .maxAge(7 * 24 * 60 * 60) // 7일 유지
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 8. Next.js가 요청 헤더에 실어 쓸 수 있도록 새 Access Token 리턴
        // (현재 프로젝트 DTO 규격인 JwtResponseDTO 활용)
        return new JwtResponseDTO(newAccessToken, newRefreshToken);
    }

    /**
     * JwtToken 삭제
     */
    @Transactional
    public void deleteRefresh(String refreshToken) {
        jwtRefreshRepository.deleteByRefresh(refreshToken);
    }

    /**
     * 유저 탈퇴시 모든 RefreshToken 삭제
     */
    @Transactional
    public void deleteByEmail(String email) {
        jwtRefreshRepository.deleteByEmail(email);
    }

}
