package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
