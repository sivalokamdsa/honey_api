package in.co.sivalokam.honey.registrations;

import jakarta.validation.constraints.*;

public record RegistrationRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
    String fullName,

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(?:\\+?91|0)?[\\s-]*[6-9](?:[\\s-]*\\d){9}$",
            message = "Enter a valid 10-digit Indian mobile number")
    String phone,

    @Email(message = "Enter a valid email")
    @Size(max = 254, message = "Email is too long")
    String email,

    @Size(max = 80, message = "City must be at most 80 characters")
    String city,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    String password,

    @NotNull(message = "Consent is required")
    @AssertTrue(message = "Please agree before registering")
    Boolean consent
){}
