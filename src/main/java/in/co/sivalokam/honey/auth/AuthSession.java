package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.registrations.Registration;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected AuthSession() {
        // required by JPA
    }

    public AuthSession(String tokenHash, Registration registration,
                       OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.registration = registration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash() { return tokenHash; }
    public Registration getRegistration() { return registration; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
