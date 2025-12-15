package biz.thonbecker.personal.content.domain;

import java.io.InputStream;

/**
 * Service for storing audio files.
 * Implementations should handle storage operations and return accessible URLs.
 */
public interface AudioStorageService {

    /**
     * Stores an audio file and returns the result with access URL.
     *
     * @param audioStream The audio data to store
     * @param contentType The content type of the audio (e.g., "audio/ogg")
     * @return AudioResult containing the CDN URL and storage details
     * @throws AudioStorageException if storage fails
     */
    AudioResult store(InputStream audioStream, String contentType);
}
