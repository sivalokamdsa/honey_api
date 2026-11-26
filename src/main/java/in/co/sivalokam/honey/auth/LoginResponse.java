package in.co.sivalokam.honey.auth;

import java.util.UUID;

public record LoginResponse(UUID id, String fullName, String phone) {
}
