package wwn.backend.handler;

import jakarta.servlet.ServletException;
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
@Qualifier("LoginSuccessHandler")
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 로그인 시 username, role 추출
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 추출한 값으로 JWT(Access/Refresh) 발급
        String accessToken = JWTUtil.createJWT(email, role, true);
        String refreshToken = JWTUtil.createJWT(email, role, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(email, refreshToken);

        // 응답 로그인 성공 후 엑세스 리프레시 토큰들 클라이언트에게 json 형식으로 보내줌 (브라우저 메모리에 저장, 출입증)
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format("{\"accessToken\":\"%s\", \"refreshToken\":\"%s\"}", accessToken, refreshToken);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
