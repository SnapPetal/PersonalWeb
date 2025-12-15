package biz.thonbecker.personal.content.domain;

import java.io.InputStream;

/**
 * Service for converting text to speech audio.
 * Implementations should handle the conversion using text-to-speech engines.
 */
public interface TextToSpeechService {

    /**
     * Converts text to speech audio stream.
     *
     * @param text The text to convert to speech
     * @param voice The voice to use for synthesis
     * @return InputStream containing the audio data
     * @throws TextToSpeechException if conversion fails
     */
    InputStream convertToSpeech(String text, Voice voice);
}
