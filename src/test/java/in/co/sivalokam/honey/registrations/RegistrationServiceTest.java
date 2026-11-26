package in.co.sivalokam.honey.registrations;

import in.co.sivalokam.honey.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private RegistrationRepository repository;
    private PasswordEncoder encoder;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RegistrationRepository.class);
        encoder = new BCryptPasswordEncoder();
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new RegistrationService(repository, encoder, clock);
        when(repository.saveAndFlush(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void storesBcryptHashNotThePlainPassword() {
        service.register(request("secret123"));

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
        verify(repository).saveAndFlush(captor.capture());
        String stored = captor.getValue().getPasswordHash();

        assertThat(stored).isNotEqualTo("secret123");
        assertThat(stored).startsWith("$2");
        assertThat(encoder.matches("secret123", stored)).isTrue();
    }

    @Test
    void rejectsPasswordLongerThan72Bytes() {
        String tooLong = "é".repeat(40); // 40 characters, 80 bytes in UTF-8
        assertThatThrownBy(() -> service.register(request(tooLong)))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    private RegistrationRequest request(String password) {
        return new RegistrationRequest("Asha Rao", "9876543210", "asha@example.com", "Pune",
                password, true);
    }
}
