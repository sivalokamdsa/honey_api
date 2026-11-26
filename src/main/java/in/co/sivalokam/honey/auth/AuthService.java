package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.common.PhoneNumbers;
import in.co.sivalokam.honey.registrations.Registration;
import in.co.sivalokam.honey.registrations.RegistrationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AuthService {

    // Same message for unknown phone, wrong password and rows without a password,
    // so the response does not reveal which phone numbers are registered.
    private static final String INVALID = "Invalid phone or password";

    private final RegistrationRepository registrations;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration sessionTtl;

    public AuthService(RegistrationRepository registrations,
                       AuthSessionRepository sessions,
                       PasswordEncoder passwordEncoder,
                       Clock clock,
                       @Value("${honey.auth.session-ttl-days:30}") long sessionTtlDays) {
        this.registrations = registrations;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.sessionTtl = Duration.ofDays(sessionTtlDays);
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        Registration r = registrations.findByPhone(PhoneNumbers.normalize(req.phone()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID));

        if (r.getPasswordHash() == null || !passwordEncoder.matches(req.password(), r.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID);
        }

        String token = SessionTokens.newToken();
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(sessionTtl);
        sessions.save(new AuthSession(SessionTokens.hash(token), r, now, expiresAt));

        return new LoginResponse(r.getId(), r.getFullName(), r.getPhone(), r.getRole(), token, expiresAt);
    }

    /** Resolves a bearer token to the current user. The role is read from the database on each request. */
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String token) {
        return sessions.findActive(SessionTokens.hash(token), OffsetDateTime.now(clock))
                .map(AuthSession::getRegistration)
                .map(r -> new AuthenticatedUser(r.getId(), r.getPhone(), r.getRole()));
    }

    @Transactional
    public void logout(String token) {
        sessions.deleteByTokenHash(SessionTokens.hash(token));
    }
}
