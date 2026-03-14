package biz.thonbecker.personal;

import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for integration tests.
 *
 * <p>Provides a PostgreSQL container and stub OAuth2 client registration
 * so tests don't need a real Cognito connection.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:18.1");
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        final var registration = ClientRegistration.withRegistrationId("cognito")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://test.auth.example.com/authorize")
                .tokenUri("https://test.auth.example.com/token")
                .userInfoUri("https://test.auth.example.com/userinfo")
                .jwkSetUri("https://test.auth.example.com/.well-known/jwks.json")
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(List.of(registration));
    }
}
