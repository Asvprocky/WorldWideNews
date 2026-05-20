package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
