package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.domain.exceptions.ImageStorageException;
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
 * Service for storing landscape images in AWS S3.
 *
 * <p>Images are uploaded with timestamped keys and served via CloudFront CDN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandscapeImageStorageService {

    private static final DateTimeFormatter KEY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.of("UTC"));
    private static final String FILE_EXTENSION = ".jpg";

    private final S3Client s3Client;

    @lombok.Getter
    @Value("${landscape.storage.bucket}")
    private String bucketName;

    @Value("${landscape.storage.cdn-domain}")
    private String cdnDomain;

    @Value("${landscape.storage.folder-prefix:landscape-plans/}")
    private String folderPrefix;

    /**
     * Stores a landscape image in S3 and returns the CDN URL.
     *
     * @param imageData Raw image bytes
     * @param contentType MIME type (e.g., "image/jpeg", "image/png")
     * @param userId User identifier for folder organization
     * @return Upload result with CDN URL and S3 key
     */
    public ImageUploadResult store(final byte[] imageData, final String contentType, final String userId) {
        try {
            final var objectKey = generateObjectKey(userId);

            log.debug("Storing landscape image to S3: bucket={}, key={}", bucketName, objectKey);

            final var putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(imageData));

            final var cdnUrl = buildCdnUrl(objectKey);
            log.info("Successfully stored landscape image: {}", cdnUrl);

            return new ImageUploadResult(cdnUrl, bucketName, objectKey);

        } catch (final Exception e) {
            log.error("Failed to store landscape image: {}", e.getMessage(), e);
            throw new ImageStorageException("Failed to store image to S3", e);
        }
    }

    /**
     * Generates a unique S3 object key based on current timestamp and user ID.
     *
     * <p>Format: landscape-plans/{userId}/YYYYMMDD-HHmmss-SSS.jpg
     *
     * @param userId User identifier
     * @return The generated object key
     */
    private String generateObjectKey(final String userId) {
        final var now = Instant.now();
        final var timestampStr = KEY_FORMATTER.format(now);
        return folderPrefix + userId + "/" + timestampStr + FILE_EXTENSION;
    }

    /**
     * Builds the CDN URL for accessing the image.
     *
     * @param objectKey The S3 object key
     * @return The full CDN URL
     */
    private String buildCdnUrl(final String objectKey) {
        return cdnDomain + "/" + objectKey;
    }

    /**
     * Result of an image upload operation.
     *
     * @param cdnUrl Public CDN URL for accessing the image
     * @param bucketName S3 bucket name
     * @param s3Key S3 object key
     */
    public record ImageUploadResult(String cdnUrl, String bucketName, String s3Key) {}
}
