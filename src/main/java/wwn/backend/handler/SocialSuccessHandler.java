package wwn.backend.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import wwn.backend.service.JwtService;
import wwn.backend.util.JWTUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 유저 정보 추출
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 추출한 유저로 리프래시 토큰 생성
        String refreshToken = JWTUtil.createJWT(email, "ROLE_" + role, false);

        // 발급한 리프래시 토큰 DB 저장
        jwtService.addRefresh(email, refreshToken);

        // 클라이언트 응답
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // 로컬 개발중은 false
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10); // 10초

        response.addCookie(refreshCookie);
        response.sendRedirect("http://localhost:5173/cookie");

    }
}
