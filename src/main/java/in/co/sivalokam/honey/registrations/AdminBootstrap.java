package in.co.sivalokam.honey.registrations;

import in.co.sivalokam.honey.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Grants ADMIN to the registration whose phone is set in HONEY_ADMIN_BOOTSTRAP_PHONE.
 * Runs on startup and does nothing when the variable is empty. The phone is never logged.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final RegistrationService registrations;
    private final String bootstrapPhone;

    public AdminBootstrap(RegistrationService registrations,
                          @Value("${honey.admin.bootstrap-phone:}") String bootstrapPhone) {
        this.registrations = registrations;
        this.bootstrapPhone = bootstrapPhone;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapPhone == null || bootstrapPhone.isBlank()) {
            return;
        }
        try {
            registrations.changeRole(bootstrapPhone, Role.ADMIN);
            log.info("Bootstrap admin role granted");
        } catch (ApiException e) {
            log.warn("Bootstrap admin phone is not registered yet; no role changed");
        }
    }
}
