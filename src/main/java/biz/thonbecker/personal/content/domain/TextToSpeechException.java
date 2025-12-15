package biz.thonbecker.personal.content.domain;

/**
 * Exception thrown when text-to-speech conversion fails.
 */
public class TextToSpeechException extends RuntimeException {

    public TextToSpeechException(String message) {
        super(message);
    }

    public TextToSpeechException(String message, Throwable cause) {
        super(message, cause);
    }
}
