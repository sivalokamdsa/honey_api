package in.co.sivalokam.honey.registrations;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistrationResponse(UUID id, OffsetDateTime createdAt) {

    static RegistrationResponse from(Registration r) {
        return new RegistrationResponse(r.getId(), r.getCreatedAt());
    }
}
