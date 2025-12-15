package biz.thonbecker.personal.content.domain;

/**
 * Exception thrown when audio storage operation fails.
 */
public class AudioStorageException extends RuntimeException {

    public AudioStorageException(String message) {
        super(message);
    }

    public AudioStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
