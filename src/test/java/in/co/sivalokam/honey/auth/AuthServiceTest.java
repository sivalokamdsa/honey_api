package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.common.ApiException;
import in.co.sivalokam.honey.registrations.Registration;
import in.co.sivalokam.honey.registrations.RegistrationRepository;
import in.co.sivalokam.honey.registrations.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private RegistrationRepository registrations;
    private AuthSessionRepository sessions;
    private PasswordEncoder encoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        registrations = mock(RegistrationRepository.class);
        sessions = mock(AuthSessionRepository.class);
        encoder = new BCryptPasswordEncoder();
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new AuthService(registrations, sessions, encoder, clock, 30);
    }

    private Registration registeredWith(String password) {
        return new Registration("Asha Rao", "+919876543210", null, null,
                encoder.encode(password), OffsetDateTime.now());
    }

    @Test
    void returnsProfileRoleAndTokenForCorrectPhoneAndPassword() {
        when(registrations.findByPhone("+919876543210")).thenReturn(Optional.of(registeredWith("secret123")));

        LoginResponse res = service.login(new LoginRequest("098765 43210", "secret123"));

        assertThat(res.fullName()).isEqualTo("Asha Rao");
        assertThat(res.phone()).isEqualTo("+919876543210");
        assertThat(res.role()).isEqualTo(Role.USER);
        assertThat(res.token()).isNotBlank();
        assertThat(res.expiresAt()).isEqualTo(OffsetDateTime.parse("2026-01-31T00:00:00Z"));
    }

    @Test
    void storesOnlyTheHashOfTheToken() {
        when(registrations.findByPhone("+919876543210")).thenReturn(Optional.of(registeredWith("secret123")));

        LoginResponse res = service.login(new LoginRequest("9876543210", "secret123"));

        verify(sessions).save(argThatHasHash(SessionTokens.hash(res.token())));
    }

    @Test
    void rejectsWrongPassword() {
        when(registrations.findByPhone("+919876543210")).thenReturn(Optional.of(registeredWith("secret123")));

        assertThatThrownBy(() -> service.login(new LoginRequest("9876543210", "wrong-pass")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(sessions, never()).save(any());
    }

    @Test
    void unknownPhoneGetsTheSameErrorAsWrongPassword() {
        when(registrations.findByPhone("+919876543210")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("9876543210", "secret123")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).isEqualTo("Invalid phone or password");
                });
    }

    @Test
    void rowWithoutPasswordCannotLogIn() {
        Registration legacy = new Registration("Old User", "+919876543210", null, null, null, OffsetDateTime.now());
        when(registrations.findByPhone("+919876543210")).thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> service.login(new LoginRequest("9876543210", "anything")))
                .isInstanceOf(ApiException.class);
    }

    private static AuthSession argThatHasHash(String hash) {
        return org.mockito.ArgumentMatchers.argThat(s -> s != null && hash.equals(s.getTokenHash()));
    }
}
