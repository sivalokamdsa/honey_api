package in.co.sivalokam.honey.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    @Query("select s from AuthSession s join fetch s.registration r " +
           "where s.tokenHash = :tokenHash and s.expiresAt > :now")
    Optional<AuthSession> findActive(@Param("tokenHash") String tokenHash, @Param("now") OffsetDateTime now);

    long deleteByTokenHash(String tokenHash);
}
