package biz.thonbecker.personal.shared.platform.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configuration for AWS SDK clients.
 * Provides configured clients for Polly (text-to-speech) and S3 (storage).
 */
@Configuration
public class AwsConfig {

    @Value("${spring.ai.bedrock.aws.region:us-east-1}")
    private String awsRegion;

    @Value("${spring.ai.bedrock.aws.access-key}")
    private String accessKey;

    @Value("${spring.ai.bedrock.aws.secret-key}")
    private String secretKey;

    /**
     * Creates a configured PollyClient bean for text-to-speech conversion.
     *
     * @return Configured PollyClient
     */
    @Bean
    public PollyClient pollyClient() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return PollyClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Creates a configured S3Client bean for audio file storage.
     *
     * @return Configured S3Client
     */
    @Bean
    public S3Client s3Client() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
