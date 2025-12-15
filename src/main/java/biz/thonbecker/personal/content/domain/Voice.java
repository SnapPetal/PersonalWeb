package biz.thonbecker.personal.content.domain;

/**
 * Supported voices for text-to-speech conversion.
 * These voices support conversational style with neural engine.
 */
public enum Voice {
    JOANNA("Joanna"),
    MATTHEW("Matthew"),
    IVY("Ivy"),
    KENDRA("Kendra"),
    KIMBERLY("Kimberly"),
    SALLI("Salli"),
    JOEY("Joey"),
    JUSTIN("Justin");

    private final String awsVoiceId;

    Voice(String awsVoiceId) {
        this.awsVoiceId = awsVoiceId;
    }

    public String getAwsVoiceId() {
        return awsVoiceId;
    }
}
