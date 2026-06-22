package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.NewsSource;

import java.util.List;
import java.util.Optional;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    List<NewsSource> findByIsActiveTrue();

    Optional<NewsSource> findFirstByCountry_CountryCodeIgnoreCase(String countryCode);
}
