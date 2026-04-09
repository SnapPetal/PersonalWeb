package biz.thonbecker.personal.skatetricks.platform;

import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class SkateTricksUploadService {

    private final S3Presigner s3Presigner;

    @Value("${skatetricks.transcoding.input-bucket:}")
    private String inputBucket;

    @Value("${skatetricks.transcoding.input-prefix:skatetricks/input/}")
    private String inputPrefix;

    @Value("${skatetricks.upload.presign-duration:15m}")
    private Duration presignDuration;

    public SkateTricksUploadService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    public PresignedUpload createPresignedUpload(String filename, String contentType) {
        String extension = getFileExtension(filename);
        String key = normalizePrefix(inputPrefix) + UUID.randomUUID() + "/" + sanitizeBaseName(filename) + extension;
        String normalizedContentType =
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(presignDuration)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(inputBucket)
                        .key(key)
                        .contentType(normalizedContentType)
                        .build())
                .build());

        return new PresignedUpload(key, presignedRequest.url().toString(), normalizedContentType);
    }

    public record PresignedUpload(String inputKey, String uploadUrl, String contentType) {}

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx).toLowerCase() : "";
    }

    private static String sanitizeBaseName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload";
        }
        String withoutExtension = filename.replaceFirst("\\.[^.]+$", "");
        String sanitized = withoutExtension.replaceAll("[^A-Za-z0-9-_]+", "-").replaceAll("-{2,}", "-");
        return sanitized.isBlank() ? "upload" : sanitized;
    }
}
