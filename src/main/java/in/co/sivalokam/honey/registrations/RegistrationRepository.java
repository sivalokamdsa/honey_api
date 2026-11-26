package in.co.sivalokam.honey.registrations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    boolean existsByPhone(String phone);

    Optional<Registration> findByPhone(String phone);
}
