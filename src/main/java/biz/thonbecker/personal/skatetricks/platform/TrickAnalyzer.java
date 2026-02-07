package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import java.util.List;

interface TrickAnalyzer {

    TrickAnalysisResult analyze(List<String> base64Frames);

    TrickAnalysisResult analyzeVideo(byte[] mp4VideoData);
}
