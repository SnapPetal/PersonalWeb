package biz.thonbecker.personal.skatetricks.infrastructure.web;

import biz.thonbecker.personal.skatetricks.api.SkateTricksFacade;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
class SkateTricksWebSocketController {

    private final SkateTricksFacade skateTricksFacade;
    private final SimpMessagingTemplate messagingTemplate;

    SkateTricksWebSocketController(SkateTricksFacade skateTricksFacade, SimpMessagingTemplate messagingTemplate) {
        this.skateTricksFacade = skateTricksFacade;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/skatetricks/analyze")
    public void analyzeFrames(FrameAnalysisRequest request) {
        log.info("Received {} frames for session {}", request.frames().size(), request.sessionId());

        try {
            TrickAnalysisResult result = skateTricksFacade.analyzeFrames(request.sessionId(), request.frames());
            messagingTemplate.convertAndSend("/topic/skatetricks/result/" + request.sessionId(), result);
        } catch (Exception e) {
            log.error("Error analyzing frames for session {}", request.sessionId(), e);
            messagingTemplate.convertAndSend(
                    "/topic/skatetricks/error/" + request.sessionId(), "Analysis failed: " + e.getMessage());
        }
    }

    record FrameAnalysisRequest(String sessionId, List<String> frames) {}
}
