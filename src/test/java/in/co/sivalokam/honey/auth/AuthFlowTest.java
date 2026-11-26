package in.co.sivalokam.honey.auth;

import in.co.sivalokam.honey.TestcontainersConfiguration;
import in.co.sivalokam.honey.registrations.RegistrationService;
import in.co.sivalokam.honey.registrations.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    MockMvc mvc;

    @Autowired
    RegistrationService registrations;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void checkoutRequiresLoginAndLogoutEndsTheSession() throws Exception {
        String phone = randomPhone();
        register(phone);

        // Anonymous: 401 in the standard ProblemDetail shape
        mvc.perform(post("/api/v1/checkout/orders")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please log in to continue"));

        // A made-up token authenticates nothing
        mvc.perform(post("/api/v1/checkout/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        // Logged in: the request passes security and fails only on body validation
        String token = login(phone).get("token").asString();
        mvc.perform(post("/api/v1/checkout/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // Logout ends the session, so the same token stops working
        mvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/checkout/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newRegistrationsAreUsersAndRoleCanBeChangedOnTheBackend() throws Exception {
        String phone = randomPhone();
        register(phone);

        assertThat(login(phone).get("role").asString()).isEqualTo("USER");

        registrations.changeRole(phone, Role.ADMIN);

        assertThat(login(phone).get("role").asString()).isEqualTo("ADMIN");
    }

    @Test
    void registrationCannotChooseItsOwnRole() throws Exception {
        String phone = randomPhone();
        String body = """
                {"fullName":"Asha Rao","phone":"%s","password":"%s","consent":true,"role":"ADMIN"}
                """.formatted(phone, PASSWORD);

        mvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        assertThat(login(phone).get("role").asString()).isEqualTo("USER");
    }

    private void register(String phone) throws Exception {
        String body = """
                {"fullName":"Asha Rao","phone":"%s","password":"%s","consent":true}
                """.formatted(phone, PASSWORD);
        mvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String phone) throws Exception {
        String body = """
                {"phone":"%s","password":"%s"}
                """.formatted(phone, PASSWORD);
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private static String randomPhone() {
        return "9" + String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }
}
