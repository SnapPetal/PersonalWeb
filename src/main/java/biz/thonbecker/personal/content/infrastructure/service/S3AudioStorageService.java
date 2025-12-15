package biz.thonbecker.personal.content.infrastructure.service;

import biz.thonbecker.personal.content.domain.AudioResult;
import biz.thonbecker.personal.content.domain.AudioStorageException;
import biz.thonbecker.personal.content.domain.AudioStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 implementation of AudioStorageService.
 * Stores audio files in AWS S3 and returns CDN URLs for access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3AudioStorageService implements AudioStorageService {

    private static final DateTimeFormatter KEY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.of("UTC"));
    private static final String FOLDER_PREFIX = "dadjokes/";
    private static final String FILE_EXTENSION = ".mp3";

    private final S3Client s3Client;

    @Value("${audio.storage.bucket:cdn-page-stack-processedmediabucket446d3976-oonhpdwdpfzq}")
    private String bucketName;

    @Value("${audio.storage.cdn-domain:https://cdn.thonbecker.com}")
    private String cdnDomain;

    @Override
    public AudioResult store(final InputStream audioStream, final String contentType) {
        try {
            final var objectKey = generateObjectKey();
            final var audioBytes = audioStream.readAllBytes();

            log.debug("Storing audio file to S3: bucket={}, key={}", bucketName, objectKey);

            final var putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(audioBytes));

            final var cdnUrl = buildCdnUrl(objectKey);
            log.info("Successfully stored audio file: {}", cdnUrl);

            return new AudioResult(cdnUrl, bucketName, objectKey);
        } catch (final IOException e) {
            log.error("Failed to read audio stream: {}", e.getMessage(), e);
            throw new AudioStorageException("Failed to read audio stream", e);
        } catch (final Exception e) {
            log.error("Failed to store audio file: {}", e.getMessage(), e);
            throw new AudioStorageException("Failed to store audio file to S3", e);
        }
    }

    /**
     * Generates a unique S3 object key based on current timestamp.
     * Format: dadjokes/YYYYMMDD-HHmmss-SSS.mp3
     *
     * @return The generated object key
     */
    private String generateObjectKey() {
        final var now = Instant.now();
        final var timestampStr = KEY_FORMATTER.format(now);
        return FOLDER_PREFIX + timestampStr + FILE_EXTENSION;
    }

    /**
     * Builds the CDN URL for accessing the audio file.
     *
     * @param objectKey The S3 object key
     * @return The full CDN URL
     */
    private String buildCdnUrl(final String objectKey) {
        return cdnDomain + "/" + objectKey;
    }
}
