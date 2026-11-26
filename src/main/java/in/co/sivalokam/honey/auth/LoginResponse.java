package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.registrations.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

/** {@code token} is shown only in this response. Send it as "Authorization: Bearer &lt;token&gt;". */
public record LoginResponse(UUID id, String fullName, String phone, Role role,
                            String token, OffsetDateTime expiresAt) {
}
