package biz.thonbecker.personal.shared.platform.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;

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

    /**
     * Creates a configured BedrockRuntimeClient bean for image generation.
     *
     * @return Configured BedrockRuntimeClient
     */
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return BedrockRuntimeClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Provides a Jackson 2.x ObjectMapper bean required by Spring AI's Bedrock Titan
     * embedding auto-configuration. Spring Boot 4 migrated to Jackson 3, so the
     * Jackson 2.x ObjectMapper is no longer auto-configured as a bean.
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
    public S3VectorsClient s3VectorsClient() {
        final var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        final var credentialsProvider = StaticCredentialsProvider.create(credentials);

        return S3VectorsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
