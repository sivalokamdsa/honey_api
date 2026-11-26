package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.common.PhoneNumbers;
import in.co.sivalokam.honey.registrations.Registration;
import in.co.sivalokam.honey.registrations.RegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    // Same message for unknown phone, wrong password and rows without a password,
    // so the response does not reveal which phone numbers are registered.
    private static final String INVALID = "Invalid phone or password";

    private final RegistrationRepository registrations;
    private final PasswordEncoder passwordEncoder;

    public AuthService(RegistrationRepository registrations, PasswordEncoder passwordEncoder) {
        this.registrations = registrations;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Registration r = registrations.findByPhone(PhoneNumbers.normalize(req.phone()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID));

        if (r.getPasswordHash() == null || !passwordEncoder.matches(req.password(), r.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID);
        }
        return new LoginResponse(r.getId(), r.getFullName(), r.getPhone());
    }
}
