package wwn.backend.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.JwtRefresh;
import wwn.backend.dto.request.JwtRefreshRequestDTO;
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
     * 소셜로그인 성공시 refreshToken -> 헤더 방식으로 응답
     */
    @Transactional
    public JwtResponseDTO cookie2Header(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        // 쿠키 리스트
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("쿠키가 존재하지 않습니다.");
        }

        // Refresh 토큰 획득
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            throw new RuntimeException("refreshToken 쿠키가 없습니다.");
        }

        // Refresh 토큰 검증
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // 정보 추출
        String email = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JWTUtil.createJWT(email, role, true);
        String newRefreshToken = JWTUtil.createJWT(email, role, false);

        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        JwtRefresh newRefreshEntity = JwtRefresh.builder()
                .email(email)
                .refresh(newRefreshToken)
                .build();

        deleteRefresh(refreshToken);
        jwtRefreshRepository.flush(); // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
        jwtRefreshRepository.save(newRefreshEntity);

        // 기존 쿠키 제거
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10);
        response.addCookie(refreshCookie);

        return new JwtResponseDTO(newAccessToken, newRefreshToken);
    }

    /**
     * RefreshToken 으로 AccessToken 발급 Rotate
     */
    @Transactional
    public JwtResponseDTO refreshRotate(JwtRefreshRequestDTO dto) {

        String refreshToken = dto.getRefreshToken();

        // Refresh 토큰 검증
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // RefreshEntity 존재 확인 (화이트리스트)
        if (!existsRefresh(refreshToken)) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // 정보 추출
        String email = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JWTUtil.createJWT(email, role, true);
        String newRefreshToken = JWTUtil.createJWT(email, role, false);

        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        JwtRefresh newRefreshEntity = JwtRefresh.builder()
                .email(email)
                .refresh(newRefreshToken)
                .build();

        deleteRefresh(refreshToken);
        jwtRefreshRepository.save(newRefreshEntity);

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
