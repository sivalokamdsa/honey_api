package in.co.sivalokam.honey.registrations;

import in.co.sivalokam.honey.common.PhoneNumbers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class RegistrationService {

    private final RegistrationRepository repository;
    private final Clock clock;

    public RegistrationService(RegistrationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Registration register(RegistrationRequest req) {
        String phone = normalizePhone(req.phone());

        if (repository.existsByPhone(phone)) {
            throw new DuplicateRegistrationException();
        }

        Registration registration = new Registration(
                req.fullName().strip(),
                phone,
                blankToNull(req.email(), true),
                blankToNull(req.city(), false),
                OffsetDateTime.now(clock));

        try {
            return repository.saveAndFlush(registration);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRegistrationException();
        }
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
