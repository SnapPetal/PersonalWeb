package biz.thonbecker.personal.skatetricks.platform;

public interface VideoTranscoder {

    TranscodedVideo convertToMp4(byte[] videoData, String originalFilename) throws VideoTranscodingException;

    TranscodedVideo convertUploadedObjectToMp4(String inputKey, String originalFilename) throws VideoTranscodingException;

    byte[] loadTranscodedVideo(String outputKey) throws VideoTranscodingException;

    record TranscodedVideo(byte[] mp4Data, String videoUrl, String outputKey) {}

    class VideoTranscodingException extends Exception {
        public VideoTranscodingException(String message) {
            super(message);
        }

        public VideoTranscodingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
