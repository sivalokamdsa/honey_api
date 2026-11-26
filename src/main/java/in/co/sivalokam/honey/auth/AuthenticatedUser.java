package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.registrations.Role;

import java.util.UUID;

/** The logged-in caller. Set as the security principal for the duration of one request. */
public record AuthenticatedUser(UUID id, String phone, Role role) {
}
