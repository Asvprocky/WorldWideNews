package wwn.backend.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.JwtRefresh;

import java.time.LocalDateTime;

public interface JwtRefreshRepository extends JpaRepository<JwtRefresh, Long> {


    Boolean existsByRefresh(String jwtRefreshToken);

    void deleteByRefresh(String jwtRefreshToken);

    void deleteByEmail(String email);

    // 특정일 지난 refresh 토큰 삭제
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime createdDate);
}
