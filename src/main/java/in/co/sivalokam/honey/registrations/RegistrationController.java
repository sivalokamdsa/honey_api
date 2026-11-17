package in.co.sivalokam.honey.registrations;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        Registration saved = service.register(request);
        return ResponseEntity
                .created(URI.create("/api/v1/registrations/" + saved.getId()))
                .body(RegistrationResponse.from(saved));
    }
}