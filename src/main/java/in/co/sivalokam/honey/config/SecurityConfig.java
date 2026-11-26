package in.co.sivalokam.honey.config;

import in.co.sivalokam.honey.auth.AuthService;
import in.co.sivalokam.honey.auth.SessionAuthenticationFilter;
import in.co.sivalokam.honey.common.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthService authService,
                                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        http
                // Stateless bearer tokens, no cookies, so CSRF does not apply.
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/registrations",
                                "/api/v1/auth/login",
                                // Razorpay calls this. The HMAC of the raw body is checked in the controller.
                                "/api/v1/webhooks/razorpay").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products",
                                "/actuator/health",
                                "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Any logged-in user
                        .requestMatchers("/api/v1/checkout/**", "/api/v1/auth/logout").authenticated()

                        // Backend admins only (no admin endpoints yet)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Everything else needs a login, which also keeps unknown paths out of anonymous reach
                        .anyRequest().authenticated())

                // Errors use the same ProblemDetail shape as the rest of the API.
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) ->
                                resolver.resolveException(request, response, null,
                                        new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue")))
                        .accessDeniedHandler((request, response, ex) ->
                                resolver.resolveException(request, response, null,
                                        new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to do that"))))

                .addFilterBefore(new SessionAuthenticationFilter(authService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
