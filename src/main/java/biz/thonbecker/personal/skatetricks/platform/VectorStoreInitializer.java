package biz.thonbecker.personal.skatetricks.platform;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.model.ConflictException;
import software.amazon.awssdk.services.s3vectors.model.DataType;
import software.amazon.awssdk.services.s3vectors.model.DistanceMetric;

/**
 * Ensures the S3 Vectors index exists at application startup.
 * Creates the index if it does not already exist; silently skips if it does.
 */
@Component
@Slf4j
class VectorStoreInitializer {

    private final S3VectorsClient s3VectorsClient;

    @Value("${skatetricks.vectorstore.bucket}")
    private String vectorBucket;

    @Value("${skatetricks.vectorstore.index}")
    private String vectorIndex;

    @Value("${skatetricks.vectorstore.dimension}")
    private int dimension;

    VectorStoreInitializer(final S3VectorsClient s3VectorsClient) {
        this.s3VectorsClient = s3VectorsClient;
    }

    @PostConstruct
    void ensureIndexExists() {
        try {
            s3VectorsClient.createIndex(r -> r.vectorBucketName(vectorBucket)
                    .indexName(vectorIndex)
                    .dataType(DataType.FLOAT32)
                    .dimension(dimension)
                    .distanceMetric(DistanceMetric.COSINE));
            log.info("Created S3 Vectors index '{}' in bucket '{}'", vectorIndex, vectorBucket);
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
}
