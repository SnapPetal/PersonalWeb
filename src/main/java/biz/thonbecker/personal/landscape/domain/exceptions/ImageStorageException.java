package biz.thonbecker.personal.landscape.domain.exceptions;

/**
 * Thrown when uploading an image to S3 fails.
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(final String message) {
        super(message);
    }

    public ImageStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
