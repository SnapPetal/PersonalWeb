package solutions.thonbecker.personal.configuration;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {
    @Value("${foosball.api.base-url}")
    private String foosballBaseUrl;

    @Value("${foosball.api.cognito.token-url}")
    private String cognitoTokenUrl;

    @Value("${foosball.api.cognito.client-id}")
    private String cognitoClientId;

    @Value("${foosball.api.cognito.client-secret}")
    private String cognitoClientSecret;

    @Value("${foosball.api.cognito.scope}")
    private String cognitoScope;

    // Simple in-memory cached token
    private volatile String cachedAccessToken;
    private volatile Instant cachedExpiry = Instant.EPOCH;

    @Bean
    public RestTemplate restTemplate() {
        final var restTemplate = new RestTemplate();

        // Add message converters
    final var messageConverters = new ArrayList<HttpMessageConverter<?>>();

        // JSON converter for application/json responses
    final var jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        messageConverters.add(jsonConverter);

        // String converter for text/plain responses
    final var stringConverter = new org.springframework.http.converter.StringHttpMessageConverter();
        stringConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN));
        messageConverters.add(stringConverter);

        restTemplate.setMessageConverters(messageConverters);

        // Add interceptor to attach Authorization Bearer token only for requests targeting the foosball host
        
    final var authInterceptor = new CognitoAuthInterceptor();

        // Copy existing interceptors and prepend our auth interceptor
    final var interceptors = new ArrayList<ClientHttpRequestInterceptor>(restTemplate.getInterceptors());
    interceptors.add(0, authInterceptor);
    restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }

    private synchronized String getAccessToken() {
        // Return cached token if still valid (with 30s buffer)
        final var now = Instant.now();
        if (cachedAccessToken != null && cachedExpiry.isAfter(now.plusSeconds(30))) {
            return cachedAccessToken;
        }

        if (cognitoTokenUrl == null || cognitoTokenUrl.isBlank() || cognitoClientId == null
                || cognitoClientId.isBlank() || cognitoClientSecret == null || cognitoClientSecret.isBlank()) {
            return null;
        }

        final var tokenClient = new RestTemplate();
        tokenClient.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        final var headers = new org.springframework.http.HttpHeaders();
        headers.setBasicAuth(cognitoClientId, cognitoClientSecret);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        var body = "grant_type=client_credentials";
        if (cognitoScope != null && !cognitoScope.isBlank()) {
            body += "&scope=" + java.net.URLEncoder.encode(cognitoScope, java.nio.charset.StandardCharsets.UTF_8);
        }

        final var requestEntity = new org.springframework.http.HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            final var response = (java.util.Map<String, Object>) tokenClient.postForObject(cognitoTokenUrl, requestEntity, java.util.Map.class);
            if (response != null && response.get("access_token") instanceof String) {
                final var token = (String) response.get("access_token");
                final var expiresObj = response.get("expires_in");
                var expiresIn = 300L; // default 5 minutes
                if (expiresObj instanceof Number) {
                    expiresIn = ((Number) expiresObj).longValue();
                } else if (expiresObj instanceof String) {
                    try {
                        expiresIn = Long.parseLong((String) expiresObj);
                    } catch (NumberFormatException ignored) {
                    }
                }

                cachedAccessToken = token;
                cachedExpiry = Instant.now().plusSeconds(expiresIn);
                return token;
            }
        } catch (Exception e) {
            // Failed to fetch token
            return null;
        }

        return null;
    }

    private class CognitoAuthInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public @NonNull ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
                @NonNull ClientHttpRequestExecution execution) throws java.io.IOException {
            URI requestUri = request.getURI();
            String foosballHost = URI.create(foosballBaseUrl).getHost();
            if (requestUri.getHost() != null && requestUri.getHost().equalsIgnoreCase(foosballHost)) {
                HttpHeaders headers = request.getHeaders();
                String token = getAccessToken();
                if (token != null && !token.isBlank()) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            }
            return execution.execute(request, body);
        }
    }
}
