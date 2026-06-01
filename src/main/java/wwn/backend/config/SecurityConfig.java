package wwn.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import wwn.backend.filter.JwtFilter;
import wwn.backend.filter.LoginFilter;
import wwn.backend.handler.LogoutSuccessHandler;
import wwn.backend.service.JwtService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthenticationSuccessHandler loginSuccessHandler;
    private final AuthenticationSuccessHandler socialSuccessHandler;
    private final JwtService jwtService;

    public SecurityConfig(
            AuthenticationConfiguration authenticationConfiguration,
            @Qualifier("loginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
            @Qualifier("SocialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
            JwtService jwtService) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.loginSuccessHandler = loginSuccessHandler;
        this.socialSuccessHandler = socialSuccessHandler;
        this.jwtService = jwtService;

    }

    // 커스텀 필터 체인을 위한 AuthenticationManager Bean 등록
    @Bean
    public AuthenticationManager AuthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 비밀번호 단방향 암호화용 Bean 등록
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 시큐리티 필터 체인
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CSRF 보안 필터 disable
        http
                .csrf(AbstractHttpConfigurer::disable);

        // CORS 설정


        // 기본 Form 기반 로그인 disable
        http
                .formLogin(AbstractHttpConfigurer::disable);


        // 기본 베이직 로그인 disable
        http
                .httpBasic(AbstractHttpConfigurer::disable);

        // oauth2 인증
        http
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(socialSuccessHandler));

        // 인가
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());

        // 커스텀 필터 체인 설정 (커스텀으로 완전히 갈아끼움)
        http
                .addFilterBefore(new LoginFilter(AuthenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);


        // 로그아웃 헨들러 기존 로그아웃 핸들러에서 추가만함 (refreshToken 삭제)
        http
                .logout(logout -> logout
                        .addLogoutHandler(new LogoutSuccessHandler(jwtService)));

        http
                .addFilterBefore(new JwtFilter(), LogoutFilter.class);

        // 예외 처리
        http
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED); // 401 응답
                        })
                        .accessDeniedHandler((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 응답
                        })
                );

        // 세션 필터 설정(stateless)
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        return http.build();
    }
}
