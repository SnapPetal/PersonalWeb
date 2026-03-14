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
 * Shared test configuration providing Testcontainers PostgreSQL and a stub
 * OAuth2 client registration so tests don't need a real Cognito connection.
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
                .clientId("test")
                .clientSecret("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://stub/authorize")
                .tokenUri("https://stub/token")
                .jwkSetUri("https://stub/jwks")
                .scope("openid")
                .build();
        return new InMemoryClientRegistrationRepository(List.of(registration));
    }
}
