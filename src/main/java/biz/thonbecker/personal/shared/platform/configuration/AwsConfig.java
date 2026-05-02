package biz.thonbecker.personal.shared.platform.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.ses.SesClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration for AWS SDK clients.
 * Provides configured clients for Polly (text-to-speech) and S3 (storage).
 */
@Configuration
public class AwsConfig {

    @Value("${PERSONAL_AWS_REGION:us-east-1}")
    private String awsRegion;

    @Value("${PERSONAL_AWS_ACCESS_KEY_ID:test}")
    private String accessKey;

    @Value("${PERSONAL_AWS_SECRET_ACCESS_KEY:test}")
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

    @Bean
    public S3Presigner s3Presigner() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public MediaConvertClient mediaConvertClient() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return MediaConvertClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Creates a configured SesClient bean for sending emails.
     *
     * @return Configured SesClient
     */
    @Bean
    public SesClient sesClient() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return SesClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Provides a Jackson ObjectMapper bean for components that need direct JSON parsing.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Creates a configured S3VectorsClient bean for vector store operations.
     *
     * @return Configured S3VectorsClient
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "skatetricks.vectorstore.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public S3VectorsClient s3VectorsClient() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return S3VectorsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
