package biz.thonbecker.personal.skatetricks.api;

import java.util.List;

public interface SkateTricksFacade {

    TrickAnalysisResult analyzeFrames(String sessionId, List<String> base64Frames);

    TrickAnalysisResult analyzeVideo(String sessionId, byte[] videoData, String originalFilename);

    /** Converts video to MP4 format, returns the MP4 bytes. */
    byte[] convertVideo(byte[] videoData, String originalFilename);

    /** Analyzes an already-converted MP4 video. */
    TrickAnalysisResult analyzeConvertedVideo(String sessionId, byte[] mp4Data);

    List<TrickAnalysisResult> getSessionHistory(String sessionId);
}
