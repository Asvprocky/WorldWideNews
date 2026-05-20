package wwn.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wwn.backend.domain.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
