package solutions.thonbecker.personal.service;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FoosballAuthRequestInterceptor implements RequestInterceptor {

    private final CognitoTokenService cognitoTokenService;

    @Override
    public void apply(RequestTemplate template) {
        try {
            String token = cognitoTokenService.getAccessToken();
            template.header("Authorization", "Bearer " + token);
        } catch (Exception e) {
            log.error("Failed to get access token for foosball API: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to authenticate with foosball API", e);
        }
    }
}
