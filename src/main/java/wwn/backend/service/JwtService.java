package wwn.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.JwtRefresh;
import wwn.backend.repository.JwtRefreshRepository;

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
