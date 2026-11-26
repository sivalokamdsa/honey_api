package in.co.sivalokam.honey.registrations;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.common.PhoneNumbers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class RegistrationService {

    // BCrypt only uses the first 72 bytes and rejects longer input.
    private static final int MAX_PASSWORD_BYTES = 72;

    private final RegistrationRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public RegistrationService(RegistrationRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Registration register(RegistrationRequest req) {
        if (req.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Some fields need fixing",
                    Map.of("password", "Password is too long"));
        }

        String phone = normalizePhone(req.phone());

        if (repository.existsByPhone(phone)) {
            throw new DuplicateRegistrationException();
        }

        Registration registration = new Registration(
                req.fullName().strip(),
                phone,
                blankToNull(req.email(), true),
                blankToNull(req.city(), false),
                passwordEncoder.encode(req.password()),
                OffsetDateTime.now(clock));

        try {
            return repository.saveAndFlush(registration);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRegistrationException();
        }
    }

    /**
     * Backend-only: changes a registration's role, e.g. to promote a user to ADMIN.
     * No HTTP endpoint calls this.
     */
    @Transactional
    public void changeRole(String phone, Role role) {
        Registration registration = repository.findByPhone(normalizePhone(phone))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration not found"));
        registration.changeRole(role);
    }

    static String normalizePhone(String raw) {
        return PhoneNumbers.normalize(raw);
    }

    private static String blankToNull(String value, boolean lowercase) {
        if (value == null || value.isBlank()) return null;
        String v = value.strip();
        return lowercase ? v.toLowerCase(Locale.ROOT) : v;
    }
}
