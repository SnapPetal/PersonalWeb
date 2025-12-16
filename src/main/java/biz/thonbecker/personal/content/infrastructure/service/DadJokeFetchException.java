package biz.thonbecker.personal.content.infrastructure.service;

/**
 * Exception thrown when dad joke fetching or processing fails.
 */
public class DadJokeFetchException extends RuntimeException {
    public DadJokeFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
