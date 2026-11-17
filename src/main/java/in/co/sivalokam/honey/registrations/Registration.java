package in.co.sivalokam.honey.registrations;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 16)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 80)
    private String city;

    @Column(name = "consent_at", nullable = false)
    private OffsetDateTime consentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;


    protected Registration() {
        // required by JPA
    }

    public Registration(String fullName, String phone, String email, String city, OffsetDateTime now) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.city = city;
        this.consentAt = now;
        this.createdAt = now;
    }
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getCity() { return city; }
    public OffsetDateTime getConsentAt() { return consentAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

}
