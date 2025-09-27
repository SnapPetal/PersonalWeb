package solutions.thonbecker.personal.service;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CognitoTokenService {

    @Value("${foosball.api.cognito.token-url}")
    private String tokenUrl;

    @Value("${foosball.api.cognito.client-id}")
    private String clientId;

    @Value("${foosball.api.cognito.client-secret}")
    private String clientSecret;

    private RestTemplate getRestTemplate() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                    .build();
            SSLConnectionSocketFactory socketFactory =
                    new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManagerShared(true)
                    .setSSLSocketFactory(socketFactory)
                    .build();
            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            return new RestTemplate(requestFactory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RestTemplate", e);
        }
    }

    @Cacheable("cognitoToken")
    public String getAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<CognitoTokenResponse> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, request, CognitoTokenResponse.class);

            if (response.getBody() != null && response.getBody().getAccessToken() != null) {
                return response.getBody().getAccessToken();
            } else {
                throw new RuntimeException("No access token in response");
            }
        } catch (HttpClientErrorException e) {
            log.error(
                    "Failed to get access token. Status: {}, Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException("Failed to authenticate with Cognito", e);
        } catch (Exception e) {
            log.error("Unexpected error getting access token", e);
            throw new RuntimeException("Failed to get access token", e);
        }
    }

    public static class CognitoTokenResponse {
        private String access_token;

        public String getAccessToken() {
            return access_token;
        }

        public void setAccessToken(String access_token) {
            this.access_token = access_token;
        }
    }
}
