package biz.thonbecker.personal.skatetricks.platform;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.ContainerSettings;
import software.amazon.awssdk.services.mediaconvert.model.ContainerType;
import software.amazon.awssdk.services.mediaconvert.model.DescribeEndpointsMode;
import software.amazon.awssdk.services.mediaconvert.model.FileGroupSettings;
import software.amazon.awssdk.services.mediaconvert.model.GetJobResponse;
import software.amazon.awssdk.services.mediaconvert.model.H264CodecLevel;
import software.amazon.awssdk.services.mediaconvert.model.H264CodecProfile;
import software.amazon.awssdk.services.mediaconvert.model.H264RateControlMode;
import software.amazon.awssdk.services.mediaconvert.model.H264Settings;
import software.amazon.awssdk.services.mediaconvert.model.Input;
import software.amazon.awssdk.services.mediaconvert.model.JobSettings;
import software.amazon.awssdk.services.mediaconvert.model.JobStatus;
import software.amazon.awssdk.services.mediaconvert.model.Mp4Settings;
import software.amazon.awssdk.services.mediaconvert.model.Output;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroup;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroupSettings;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroupType;
import software.amazon.awssdk.services.mediaconvert.model.VideoCodec;
import software.amazon.awssdk.services.mediaconvert.model.VideoCodecSettings;
import software.amazon.awssdk.services.mediaconvert.model.VideoDescription;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

@Component
@Slf4j
class AwsMediaConvertVideoTranscoder implements VideoTranscoder {

    private final S3Client s3Client;
    private final MediaConvertClient mediaConvertDiscoveryClient;

    @Value("${skatetricks.transcoding.input-bucket:}")
    private String inputBucket;

    @Value("${skatetricks.transcoding.output-bucket:}")
    private String outputBucket;

    @Value("${skatetricks.transcoding.mediaconvert-role-arn:}")
    private String mediaConvertRoleArn;

    @Value("${skatetricks.transcoding.endpoint:}")
    private String mediaConvertEndpoint;

    @Value("${skatetricks.transcoding.input-prefix:skatetricks/input/}")
    private String inputPrefix;

    @Value("${skatetricks.transcoding.output-prefix:skatetricks/output/}")
    private String outputPrefix;

    @Value("${skatetricks.transcoding.cdn-domain:https://cdn.thonbecker.com}")
    private String cdnDomain;

    @Value("${skatetricks.transcoding.poll-interval:PT5S}")
    private Duration pollInterval;

    @Value("${skatetricks.transcoding.timeout:PT5M}")
    private Duration timeout;

    private volatile MediaConvertClient mediaConvertExecutionClient;

    AwsMediaConvertVideoTranscoder(S3Client s3Client, MediaConvertClient mediaConvertDiscoveryClient) {
        this.s3Client = s3Client;
        this.mediaConvertDiscoveryClient = mediaConvertDiscoveryClient;
    }

    @Override
    public TranscodedVideo convertToMp4(byte[] videoData, String originalFilename) throws VideoTranscodingException {
        validateConfiguration();

        String sourceExtension = getFileExtension(originalFilename);
        String safeBaseName = sanitizeBaseName(originalFilename);
        String jobId = UUID.randomUUID().toString();
        String inputKey = normalizePrefix(inputPrefix) + jobId + "/" + safeBaseName + sourceExtension;
        String outputKeyPrefix = normalizePrefix(outputPrefix) + jobId + "/";

        try {
            uploadInput(videoData, inputKey, originalFilename);

            String mediaConvertJobId = startMediaConvertJob(inputKey, outputKeyPrefix);

            waitForCompletion(mediaConvertJobId);
            String outputKey = resolveOutputKey(outputKeyPrefix);
            byte[] mp4Data = downloadOutput(outputKey);

            log.info(
                    "Transcoded video via MediaConvert: {} -> {} ({} bytes)",
                    originalFilename,
                    outputKey,
                    mp4Data.length);
            return new TranscodedVideo(mp4Data, buildVideoUrl(outputKey), outputKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoTranscodingException("MediaConvert transcoding was interrupted", e);
        } catch (Exception e) {
            throw new VideoTranscodingException("MediaConvert transcoding failed: " + e.getMessage(), e);
        } finally {
            cleanupInputObject(inputKey);
        }
    }

    @Override
    public TranscodedVideo convertUploadedObjectToMp4(String inputKey, String originalFilename) throws VideoTranscodingException {
        validateConfiguration();

        String jobId = UUID.randomUUID().toString();
        String outputKeyPrefix = normalizePrefix(outputPrefix) + jobId + "/";

        try {
            if (isMp4(originalFilename)) {
                String outputKey = outputKeyPrefix + sanitizeBaseName(originalFilename) + ".mp4";
                s3Client.copyObject(CopyObjectRequest.builder()
                        .sourceBucket(inputBucket)
                        .sourceKey(inputKey)
                        .destinationBucket(outputBucket)
                        .destinationKey(outputKey)
                        .contentType("video/mp4")
                        .metadataDirective("REPLACE")
                        .build());
                byte[] mp4Data = downloadOutput(outputKey);
                return new TranscodedVideo(mp4Data, buildVideoUrl(outputKey), outputKey);
            }

            String mediaConvertJobId = startMediaConvertJob(inputKey, outputKeyPrefix);
            waitForCompletion(mediaConvertJobId);
            String outputKey = resolveOutputKey(outputKeyPrefix);
            byte[] mp4Data = downloadOutput(outputKey);
            return new TranscodedVideo(mp4Data, buildVideoUrl(outputKey), outputKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoTranscodingException("MediaConvert transcoding was interrupted", e);
        } catch (Exception e) {
            throw new VideoTranscodingException("MediaConvert transcoding failed: " + e.getMessage(), e);
        } finally {
            cleanupInputObject(inputKey);
        }
    }

    @Override
    public byte[] loadTranscodedVideo(String outputKey) throws VideoTranscodingException {
        if (isBlank(outputKey)) {
            throw new VideoTranscodingException("Missing transcoded video output key");
        }
        try {
            return downloadOutput(outputKey);
        } catch (Exception e) {
            throw new VideoTranscodingException("Failed to load transcoded video: " + e.getMessage(), e);
        }
    }

    private void uploadInput(byte[] videoData, String inputKey, String originalFilename) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(inputBucket)
                        .key(inputKey)
                        .contentType(contentTypeFor(originalFilename))
                        .build(),
                RequestBody.fromBytes(videoData));
    }

    private String startMediaConvertJob(String inputKey, String outputKeyPrefix) {
        String outputDestination = "s3://" + outputBucket + "/" + outputKeyPrefix;
        return executionClient()
                .createJob(r -> r.role(mediaConvertRoleArn)
                        .settings(JobSettings.builder()
                                .inputs(Input.builder()
                                        .fileInput("s3://" + inputBucket + "/" + inputKey)
                                        .build())
                                .outputGroups(OutputGroup.builder()
                                        .name("File Group")
                                        .outputGroupSettings(OutputGroupSettings.builder()
                                                .type(OutputGroupType.FILE_GROUP_SETTINGS)
                                                .fileGroupSettings(FileGroupSettings.builder()
                                                        .destination(outputDestination)
                                                        .build())
                                                .build())
                                        .outputs(Output.builder()
                                                .containerSettings(ContainerSettings.builder()
                                                        .container(ContainerType.MP4)
                                                        .mp4Settings(Mp4Settings.builder().build())
                                                        .build())
                                                .videoDescription(VideoDescription.builder()
                                                        .codecSettings(VideoCodecSettings.builder()
                                                                .codec(VideoCodec.H_264)
                                                                .h264Settings(H264Settings.builder()
                                                                        .rateControlMode(H264RateControlMode.QVBR)
                                                                        .codecProfile(H264CodecProfile.MAIN)
                                                                        .codecLevel(H264CodecLevel.AUTO)
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .extension("mp4")
                                                .build())
                                        .build())
                                .build()))
                .job()
                .id();
    }

    private void waitForCompletion(String jobId) throws InterruptedException, VideoTranscodingException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            GetJobResponse response = executionClient().getJob(r -> r.id(jobId));
            JobStatus status = response.job().status();

            if (status == JobStatus.COMPLETE) {
                return;
            }
            if (status == JobStatus.ERROR || status == JobStatus.CANCELED) {
                String details = Objects.toString(response.job().errorMessage(), status.toString());
                throw new VideoTranscodingException("MediaConvert job failed: " + details);
            }

            Thread.sleep(Math.max(250L, pollInterval.toMillis()));
        }

        throw new VideoTranscodingException("MediaConvert job timed out after " + timeout);
    }

    private String resolveOutputKey(String outputKeyPrefix) throws VideoTranscodingException {
        List<String> keys = s3Client
                .listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(outputBucket)
                        .prefix(outputKeyPrefix)
                        .build())
                .contents()
                .stream()
                .map(item -> item.key())
                .filter(key -> key.endsWith(".mp4"))
                .sorted(Comparator.naturalOrder())
                .toList();

        if (keys.isEmpty()) {
            throw new VideoTranscodingException("MediaConvert completed without producing an MP4 output");
        }

        return keys.get(0);
    }

    private byte[] downloadOutput(String outputKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(outputBucket)
                        .key(outputKey)
                        .build())
                .asByteArray();
    }

    private void cleanupInputObject(String inputKey) {
        try {
            s3Client.deleteObject(r -> r.bucket(inputBucket).key(inputKey));
        } catch (Exception e) {
            log.warn("Failed to clean up MediaConvert input object {}", inputKey, e);
        }
    }

    private synchronized MediaConvertClient executionClient() {
        if (mediaConvertExecutionClient != null) {
            return mediaConvertExecutionClient;
        }

        String endpoint = mediaConvertEndpoint;
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = mediaConvertDiscoveryClient
                    .describeEndpoints(r -> r.maxResults(1).mode(DescribeEndpointsMode.DEFAULT))
                    .endpoints()
                    .get(0)
                    .url();
        }

        mediaConvertExecutionClient = MediaConvertClient.builder()
                .region(mediaConvertDiscoveryClient.serviceClientConfiguration().region())
                .credentialsProvider(mediaConvertDiscoveryClient.serviceClientConfiguration().credentialsProvider())
                .endpointOverride(URI.create(endpoint))
                .build();
        return mediaConvertExecutionClient;
    }

    private void validateConfiguration() throws VideoTranscodingException {
        if (isBlank(inputBucket) || isBlank(outputBucket) || isBlank(mediaConvertRoleArn)) {
            throw new VideoTranscodingException(
                    "Skatetricks transcoding is not configured. Set skatetricks.transcoding.input-bucket, "
                            + "skatetricks.transcoding.output-bucket, and skatetricks.transcoding.mediaconvert-role-arn.");
        }
    }

    private static boolean isMp4(String originalFilename) {
        return ".mp4".equalsIgnoreCase(getFileExtension(originalFilename));
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

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private static String contentTypeFor(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return switch (extension) {
            case ".mov" -> "video/quicktime";
            case ".webm" -> "video/webm";
            case ".avi" -> "video/x-msvideo";
            case ".mkv" -> "video/x-matroska";
            case ".mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildVideoUrl(String outputKey) {
        return cdnDomain.replaceAll("/+$", "") + "/" + outputKey;
    }
}
