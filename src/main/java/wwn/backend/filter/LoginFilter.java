package wwn.backend.filter;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StreamUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginFilter extends AbstractAuthenticationProcessingFilter {

    // 1. 우리 서비스의 아이디 Key 이름은 "email"이고, 비밀번호 Key 이름은 "password"라고 정의함
    public static final String SPRING_SECURITY_FORM_EMAIL_KEY = "email";
    public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";

    // 2. 이 필터는 반드시 [POST 방식]의 [/login] 주소로 들어오는 요청만 가로채겠다고 과녁을 설정함
    private static final RequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/login");

    // 3. 필터 내부에서 유동적으로 쓸 수 있게 변수(parameter)에 1번에서 만든 "email"과 "password" 문자열을 대입함
    private String emailParameter = SPRING_SECURITY_FORM_EMAIL_KEY;
    private String passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY;

    // 4. 생성자: 부모 필터에게 "POST /login 요청을 가로채고, 검증은 이 매니저에게 시켜라" 하고 세팅을 완료함
    public LoginFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    /**
     * [핵심] 인증 시도 메서드
     * POST /login 요청 시 시큐리티가 가장 먼저 실행함.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // POST 요청이 아니면 예외를 던지고 차단함
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        // 클라이언트가 보낸 데이터를 Key-Value 쌍으로 쪼개 담을 Map 준비
        Map<String, String> loginMap;

        try {
            // [JSON 데이터 변환 과정]
            // Jackson 번역기(ObjectMapper) 생성 (JSON ↔ 자바 객체 변환용)
            ObjectMapper objectMapper = new ObjectMapper();

            // 네트워크를 통해 클라이언트가 보낸 HTTP 바디의 원시 바이트 데이터 스트림을 엶
            ServletInputStream inputStream = request.getInputStream();

            // 바이트 데이터를 사람이 읽을 수 있는 UTF-8 형식의 문자열(JSON 글자)로 변환
            String messageBody = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

            // JSON 글자 뭉치를 자바 Map 구조로 파싱하여 대입
            loginMap = objectMapper.readValue(messageBody, new TypeReference<>() {
            });

        } catch (IOException e) {
            // 파싱 실패 시 런타임 에러를 던짐
            throw new RuntimeException(e);
        }

        // 이름표("email") 기준으로 사용자가 입력한 진짜 이메일 값을 꺼냄
        String email = loginMap.get(emailParameter);
        email = (email != null) ? email.trim() : ""; // 앞뒤 공백 제거

        // 이름표("password") 기준으로 사용자가 입력한 진짜 비밀번호 값을 꺼냄
        String password = loginMap.get(passwordParameter);
        password = (password != null) ? password : "";

        // 추출한 이메일과 비밀번호로 시큐리티 표준 규격 상자인 '토큰'을 생성함
        // 비밀번호 검증 전이므로 인증되지 않은 상태(unauthenticated)로 생성됨
        UsernamePasswordAuthenticationToken authRequest = UsernamePasswordAuthenticationToken.unauthenticated(email,
                password);

        // 요청 부가 정보(IP, 세션 등)를 토큰에 기록함
        setDetails(request, authRequest);

        // 총괄 매니저(AuthenticationManager)에게 토큰을 넘기며 검증을 맡김
        // 내부적으로 Provider를 거쳐 커스텀한 UserDetailsService의 loadUser가 호출됨
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    /**
     * [부가 정보 기록]
     * 현재 요청을 보낸 클라이언트의 IP 주소나 세션 ID 등을 토큰에 저장함.
     * 향후 로깅이나 보안 추적 시 활용됨.
     */
    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }


}
