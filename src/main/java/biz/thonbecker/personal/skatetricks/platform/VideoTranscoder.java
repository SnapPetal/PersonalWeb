package biz.thonbecker.personal.skatetricks.platform;

public interface VideoTranscoder {

    TranscodedVideo convertToMp4(byte[] videoData, String originalFilename) throws VideoTranscodingException;

    TranscodedVideo convertUploadedObjectToMp4(String inputKey, String originalFilename) throws VideoTranscodingException;

    record TranscodedVideo(byte[] mp4Data, String videoUrl) {}

    class VideoTranscodingException extends Exception {
        public VideoTranscodingException(String message) {
            super(message);
        }

        public VideoTranscodingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
