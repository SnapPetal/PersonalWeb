package biz.thonbecker.personal.content.platform.service;

import biz.thonbecker.personal.content.domain.TextToSpeechException;
import biz.thonbecker.personal.content.domain.TextToSpeechService;
import biz.thonbecker.personal.content.domain.Voice;
import java.io.InputStream;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.TextType;

/**
 * AWS Polly implementation of TextToSpeechService.
 * Converts text to speech using AWS Polly Neural engine with SSML formatting
 * for natural joke delivery with appropriate pauses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PollyTextToSpeechService implements TextToSpeechService {

    private static final Pattern QUESTION_PATTERN = Pattern.compile("^(.*?\\?)\\s*([\\s\\S]+)$");
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("^(.*?)\\n+([\\s\\S]+)$");
    private static final Pattern DASH_PATTERN = Pattern.compile("^(.*?)\\s*[-–—:]\\s*([\\s\\S]+)$");

    private final PollyClient pollyClient;

    @Override
    public InputStream convertToSpeech(final String text, final Voice voice) {
        try {
            final var ssmlText = formatJokeWithSsml(text);
            log.debug("Converting text to speech with voice: {}", voice);

            final var request = SynthesizeSpeechRequest.builder()
                    .text(ssmlText)
                    .textType(TextType.SSML)
                    .voiceId(voice.getAwsVoiceId())
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.NEURAL)
                    .build();

            final var response = pollyClient.synthesizeSpeech(request);

            log.info("Successfully converted text to speech using voice: {} in MP3 format", voice);
            return response;
        } catch (final Exception e) {
            log.error("Failed to convert text to speech: {}", e.getMessage(), e);
            throw new TextToSpeechException("Failed to convert text to speech", e);
        }
    }

    /**
     * Formats joke text with SSML for natural conversational delivery.
     * Adds pauses and slight rate changes for better comedic timing.
     * Uses only SSML features supported by AWS Polly Neural voices.
     *
     * @param text The joke text to format
     * @return SSML-formatted text with enhanced vocal delivery
     */
    private String formatJokeWithSsml(final String text) {
        final var escapedText = escapeXml(text);

        // Pattern 1: Question mark followed by text (typical Q&A joke)
        final var questionMatcher = QUESTION_PATTERN.matcher(escapedText);
        if (questionMatcher.matches()) {
            final var setup = questionMatcher.group(1).trim();
            final var punchline = questionMatcher.group(2).trim();
            return "<speak>"
                    + setup + "?"
                    + "<break time=\"1.2s\"/>"
                    + "<prosody rate=\"95%\">" + punchline + "</prosody>"
                    + "</speak>";
        }

        // Pattern 2: Newline separating setup and punchline
        final var newlineMatcher = NEWLINE_PATTERN.matcher(escapedText);
        if (newlineMatcher.matches()) {
            final var setup = newlineMatcher.group(1).trim();
            final var punchline = newlineMatcher.group(2).trim();
            return "<speak>"
                    + setup
                    + "<break time=\"1s\"/>"
                    + "<prosody rate=\"95%\">" + punchline + "</prosody>"
                    + "</speak>";
        }

        // Pattern 3: Dash or colon separating setup and punchline
        final var dashMatcher = DASH_PATTERN.matcher(escapedText);
        if (dashMatcher.matches()) {
            final var setup = dashMatcher.group(1).trim();
            final var punchline = dashMatcher.group(2).trim();
            return "<speak>"
                    + setup
                    + "<break time=\"1s\"/>"
                    + "<prosody rate=\"95%\">" + punchline + "</prosody>"
                    + "</speak>";
        }

        // Default: Just plain text in speak tags
        return "<speak>" + escapedText + "</speak>";
    }

    /**
     * Escapes XML special characters for SSML.
     *
     * @param text The text to escape
     * @return Escaped text safe for SSML
     */
    private String escapeXml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
