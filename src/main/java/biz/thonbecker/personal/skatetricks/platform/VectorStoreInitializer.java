package biz.thonbecker.personal.skatetricks.platform;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.model.ConflictException;
import software.amazon.awssdk.services.s3vectors.model.DataType;
import software.amazon.awssdk.services.s3vectors.model.DistanceMetric;
import software.amazon.awssdk.services.s3vectors.model.NotFoundException;

/**
 * Ensures the S3 Vectors index exists at application startup.
 * Creates the index if it does not already exist; silently skips if it does.
 */
@Component
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "skatetricks.vectorstore.enabled",
        havingValue = "true",
        matchIfMissing = true)
class VectorStoreInitializer {

    private static final int DELETE_WAIT_ATTEMPTS = 30;
    private static final Duration DELETE_WAIT_INTERVAL = Duration.ofSeconds(2);

    private final S3VectorsClient s3VectorsClient;

    @Value("${skatetricks.vectorstore.bucket}")
    private String vectorBucket;

    @Value("${skatetricks.vectorstore.index}")
    private String vectorIndex;

    @Value("${skatetricks.vectorstore.dimension}")
    private int dimension;

    @Value("${skatetricks.vectorstore.recreate:false}")
    private boolean recreateIndex;

    VectorStoreInitializer(final S3VectorsClient s3VectorsClient) {
        this.s3VectorsClient = s3VectorsClient;
    }

    @PostConstruct
    void ensureIndexExists() {
        if (recreateIndex) {
            final var deleted = deleteIndexIfExists();
            if (deleted) {
                waitForIndexDeletion();
            }
        }
        createIndexIfNotExists();
        validateIndexConfiguration();
    }

    private boolean deleteIndexIfExists() {
        try {
            s3VectorsClient.deleteIndex(r -> r.vectorBucketName(vectorBucket).indexName(vectorIndex));
            log.info("Deleted S3 Vectors index '{}' from bucket '{}' for recreation", vectorIndex, vectorBucket);
            return true;
        } catch (NotFoundException e) {
            log.info("S3 Vectors index '{}' does not exist, nothing to delete", vectorIndex);
            return false;
        } catch (Exception e) {
            log.warn(
                    "Could not delete S3 Vectors index '{}' in bucket '{}': {}",
                    vectorIndex,
                    vectorBucket,
                    e.getMessage());
            return false;
        }
    }

    private void createIndexIfNotExists() {
        try {
            s3VectorsClient.createIndex(r -> r.vectorBucketName(vectorBucket)
                    .indexName(vectorIndex)
                    .dataType(DataType.FLOAT32)
                    .dimension(dimension)
                    .distanceMetric(DistanceMetric.COSINE));
            log.info(
                    "Created S3 Vectors index '{}' in bucket '{}' with dimension {}",
                    vectorIndex,
                    vectorBucket,
                    dimension);
        } catch (ConflictException e) {
            log.info("S3 Vectors index '{}' already exists in bucket '{}'", vectorIndex, vectorBucket);
        } catch (Exception e) {
            log.warn(
                    "Could not initialize S3 Vectors index '{}' in bucket '{}': {}",
                    vectorIndex,
                    vectorBucket,
                    e.getMessage());
        }
    }

    private void waitForIndexDeletion() {
        for (var attempt = 1; attempt <= DELETE_WAIT_ATTEMPTS; attempt++) {
            try {
                s3VectorsClient.getIndex(r -> r.vectorBucketName(vectorBucket).indexName(vectorIndex));
                sleepBeforeNextDeleteCheck(attempt);
            } catch (NotFoundException e) {
                log.info("Confirmed S3 Vectors index '{}' was deleted from bucket '{}'", vectorIndex, vectorBucket);
                return;
            }
        }
        log.warn(
                "Timed out waiting for S3 Vectors index '{}' in bucket '{}' to be deleted before recreation",
                vectorIndex,
                vectorBucket);
    }

    private void sleepBeforeNextDeleteCheck(final int attempt) {
        try {
            log.info(
                    "Waiting for S3 Vectors index '{}' deletion before recreation, attempt {}/{}",
                    vectorIndex,
                    attempt,
                    DELETE_WAIT_ATTEMPTS);
            Thread.sleep(DELETE_WAIT_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for S3 Vectors index deletion", e);
        }
    }

    private void validateIndexConfiguration() {
        try {
            final var index = s3VectorsClient
                    .getIndex(r -> r.vectorBucketName(vectorBucket).indexName(vectorIndex))
                    .index();
            if (!Integer.valueOf(dimension).equals(index.dimension())) {
                log.warn(
                        "S3 Vectors index '{}' dimension is {}, expected {}. Set skatetricks.vectorstore.recreate=true to rebuild it.",
                        vectorIndex,
                        index.dimension(),
                        dimension);
            }
        } catch (Exception e) {
            log.warn(
                    "Could not validate S3 Vectors index '{}' in bucket '{}': {}",
                    vectorIndex,
                    vectorBucket,
                    e.getMessage());
        }
    }
}
