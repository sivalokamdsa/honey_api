package in.co.sivalokam.honey.registrations;

public class DuplicateRegistrationException extends RuntimeException {

    public DuplicateRegistrationException() {
        super("This phone number is already registered");
    }
}