package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.JwtRefresh;

public interface JwtRefreshRepository extends JpaRepository<JwtRefresh, Long> {


    Boolean existsByRefresh(String jwtRefreshToken);

    void deleteByRefresh(String jwtRefreshToken);

    void deleteByEmail(String email);
}
