package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.api.TrickSequenceEntry;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

@Component
@Slf4j
class BedrockTrickAnalyzer implements TrickAnalyzer {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final PoseEstimationService poseEstimationService;

    @Autowired(required = false)
    BedrockTrickAnalyzer(ChatModel chatModel, PoseEstimationService poseEstimationService) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
        this.poseEstimationService = poseEstimationService;
    }

    @Override
    public TrickAnalysisResult analyze(List<String> base64Frames) {
        if (chatModel == null) {
            log.warn("ChatModel not configured, returning fallback result");
            return fallback();
        }

        try {
            List<Media> mediaList = base64Frames.stream()
                    .map(frame -> Media.builder()
                            .mimeType(MimeTypeUtils.IMAGE_JPEG)
                            .data(Base64.getDecoder().decode(frame))
                            .build())
                    .toList();

            String poseDataText = buildPoseDataText(base64Frames);

            String systemPrompt = """
                    You are an expert skateboarding coach and trick judge. You will receive sequential frames \
                    extracted at even intervals from a skateboarding video clip.

                    CRITICAL DETECTION RULES:
                    1. If the board LEAVES THE GROUND completely at any point, it is NOT Manual or Cruising
                    2. OLLIE: Look for tail pop (tail hits ground), board rises, ALL WHEELS off ground, lands back down
                    3. 180 variants: Same as ollie but rider/board rotates 180° during the air time
                    4. GRINDS: Board makes contact with rail/ledge edge - look for sparks or sliding on metal/concrete edge
                    5. MANUAL: Rider stays on ground but front wheels are lifted high. Must see sustained nose-up balance.
                    6. DROP_IN: If early frames show rider at top of ramp/bowl/halfpipe with board on coping edge, then transitioning down the ramp - this is DROP_IN
                    7. CRUISING: ONLY choose this if rider is on flat ground the ENTIRE clip with no ramp/transition/trick visible

                    MULTIPLE TRICKS: If the clip contains more than one trick performed in
                    sequence, identify ALL of them in order. The primary trick is the most
                    significant one (not just the first). Return them in trickSequence.

                    If you see the board airborne (all wheels off ground), choose OLLIE or a flip/spin variation.
                    If the board contacts a rail/ledge and slides, choose a GRIND variation.
                    NEVER choose MANUAL if the board leaves the ground completely.

                    ANALYSIS APPROACH - examine the frames in order and look for:
                    1. SETUP: Rider stance, foot positioning on the board, approach speed, body posture
                    2. POP/INITIATION: Tail pop, foot slide, weight shift that starts the trick
                    3. AIRBORNE PHASE: Board rotation (flip axis, spin axis), board height, rider's feet relative to board
                    4. CATCH/LANDING: Feet back on board, knees bent to absorb, rolling away clean

                    KEY VISUAL CUES:
                    - Ollie: tail snaps down, front foot slides up, board levels in air, no rotation
                    - Frontside 180: ollie with 180° rotation, rider's chest faces forward during spin
                    - Backside 180: ollie with 180° rotation, rider's back faces forward during spin
                    - Kickflip: same as ollie but board flips along length axis (grip tape flashes), front foot flicks off heel-side edge
                    - Heelflip: board flips opposite direction from kickflip, front foot flicks off toe-side edge
                    - Pop Shuvit: board spins 180° flat (no flip), rider's feet stay relatively still
                    - Tre Flip (360 flip): board does kickflip + 360 shuvit simultaneously
                    - Boardslide: board slides perpendicular across an obstacle/rail
                    - 50-50 Grind: both trucks on an edge/rail, moving along it
                    - 5-0 Grind: back truck only on edge, nose lifted while grinding
                    - Nosegrind: front truck only on edge, tail lifted while grinding
                    - Manual: riding on back wheels only, nose lifted, balancing - WHEELS NEVER LEAVE GROUND
                    - Cruising: normal flat-ground riding with all 4 wheels on ground, NO ramp or transition visible
                    - Drop In: rider starts at TOP of ramp/halfpipe/bowl with tail on coping edge, then leans forward and rides DOWN the transition. Look for: curved ramp surface, rider going from horizontal to angled downward, coping/edge at top of ramp

                    Known trick enum values and descriptions:
                    %s

                    Respond ONLY with valid JSON (no markdown, no explanation):
                    {"trick": "ENUM_NAME", "confidence": 0-100, "formScore": 0-100, "feedback": ["observation 1", "observation 2", "suggestion"], "trickSequence": [{"trick": "DROP_IN", "timeframe": "frames 1-8", "confidence": 90}, {"trick": "KICKFLIP", "timeframe": "frames 12-20", "confidence": 85}]}

                    The "trick" field is the primary/most significant trick. The "trickSequence" lists ALL tricks in chronological order.
                    If only one trick is visible, trickSequence should contain just that one entry.
                    Use the ENUM_NAME exactly as listed above (e.g., OLLIE, KICKFLIP, POP_SHUVIT, TREFLIP, FRONTSIDE_180, BACKSIDE_180, FIVE_O, NOSEGRIND, CRUISING, DROP_IN). Use UNKNOWN only if you truly cannot identify the trick.
                    """.formatted(TrickCatalog.buildTrickDescriptions()) + poseDataText;

            String userPrompt =
                    ("These %d images are sequential frames from a skateboarding video, evenly spaced in time. "
                                    + "Frame 1 is earliest, frame %d is latest. Analyze the full progression of movement across all frames and identify all tricks performed in sequence.")
                            .formatted(base64Frames.size(), base64Frames.size());

            UserMessage userMessage =
                    UserMessage.builder().text(userPrompt).media(mediaList).build();
            SystemMessage systemMessage = new SystemMessage(systemPrompt);

            Prompt prompt = new Prompt(List.<Message>of(systemMessage, userMessage));
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            log.info("AI analysis response: {}", response);
            return parseResponse(response);

        } catch (Exception e) {
            log.error("Error analyzing skateboard trick frames", e);
            return fallback();
        }
    }

    @Override
    public TrickAnalysisResult analyzeVideo(byte[] mp4VideoData) {
        if (chatModel == null) {
            log.warn("ChatModel not configured, returning fallback result");
            return fallback();
        }

        try {
            Media videoMedia = Media.builder()
                    .mimeType(MimeType.valueOf("video/mp4"))
                    .data(mp4VideoData)
                    .build();

            String systemPrompt = """
                    You are an expert skateboarding coach and trick judge. You will receive a video clip \
                    of a skateboarding trick attempt.

                    CRITICAL DETECTION RULES:
                    1. If the board LEAVES THE GROUND completely at any point, it is NOT Manual or Cruising
                    2. OLLIE: Look for tail pop (tail hits ground), board rises, ALL WHEELS off ground, lands back down
                    3. 180 variants: Same as ollie but rider/board rotates 180° during the air time
                    4. GRINDS: Board makes contact with rail/ledge edge - look for sparks or sliding on metal/concrete edge
                    5. MANUAL: Rider stays on ground but front wheels are lifted high. Must see sustained nose-up balance.
                    6. CRUISING: All 4 wheels stay on ground, rider is simply rolling

                    MULTIPLE TRICKS: If the clip contains more than one trick performed in
                    sequence, identify ALL of them in order. The primary trick is the most
                    significant one (not just the first). Return them in trickSequence.

                    If you see the board airborne (all wheels off ground), choose OLLIE or a flip/spin variation.
                    If the board contacts a rail/ledge and slides, choose a GRIND variation.
                    NEVER choose MANUAL if the board leaves the ground completely.

                    ANALYSIS APPROACH - watch the video and look for:
                    1. SETUP: Rider stance, foot positioning on the board, approach speed, body posture
                    2. POP/INITIATION: Tail pop, foot slide, weight shift that starts the trick
                    3. AIRBORNE PHASE: Board rotation (flip axis, spin axis), board height, rider's feet relative to board
                    4. CATCH/LANDING: Feet back on board, knees bent to absorb, rolling away clean

                    KEY VISUAL CUES:
                    - Ollie: tail snaps down, front foot slides up, board levels in air, no rotation
                    - Frontside 180: ollie with 180° rotation, rider's chest faces forward during spin
                    - Backside 180: ollie with 180° rotation, rider's back faces forward during spin
                    - Kickflip: same as ollie but board flips along length axis (grip tape flashes), front foot flicks off heel-side edge
                    - Heelflip: board flips opposite direction from kickflip, front foot flicks off toe-side edge
                    - Pop Shuvit: board spins 180° flat (no flip), rider's feet stay relatively still
                    - Tre Flip (360 flip): board does kickflip + 360 shuvit simultaneously
                    - Boardslide: board slides perpendicular across an obstacle/rail
                    - 50-50 Grind: both trucks on an edge/rail, moving along it
                    - 5-0 Grind: back truck only on edge, nose lifted while grinding
                    - Nosegrind: front truck only on edge, tail lifted while grinding
                    - Manual: riding on back wheels only, nose lifted, balancing - WHEELS NEVER LEAVE GROUND
                    - Cruising: normal riding with all 4 wheels on ground
                    - Drop In: transitioning from standing on coping into riding down a ramp/bowl

                    Known trick enum values and descriptions:
                    %s

                    Respond ONLY with valid JSON (no markdown, no explanation):
                    {"trick": "ENUM_NAME", "confidence": 0-100, "formScore": 0-100, "feedback": ["observation 1", "observation 2", "suggestion"], "trickSequence": [{"trick": "DROP_IN", "timeframe": "0:00-0:02", "confidence": 90}, {"trick": "KICKFLIP", "timeframe": "0:03-0:05", "confidence": 85}]}

                    The "trick" field is the primary/most significant trick. The "trickSequence" lists ALL tricks in chronological order.
                    If only one trick is visible, trickSequence should contain just that one entry.
                    Use the ENUM_NAME exactly as listed above (e.g., OLLIE, KICKFLIP, POP_SHUVIT, TREFLIP, FRONTSIDE_180, BACKSIDE_180, FIVE_O, NOSEGRIND, CRUISING). Use UNKNOWN only if you truly cannot identify the trick.
                    """.formatted(TrickCatalog.buildTrickDescriptions());

            String userPrompt = "Watch this skateboarding video and identify the trick being performed. "
                    + "Pay close attention to the board movement, especially whether it leaves the ground.";

            UserMessage userMessage =
                    UserMessage.builder().text(userPrompt).media(videoMedia).build();
            SystemMessage systemMessage = new SystemMessage(systemPrompt);

            Prompt prompt = new Prompt(List.<Message>of(systemMessage, userMessage));
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            log.info("AI video analysis response: {}", response);
            return parseResponse(response);

        } catch (Exception e) {
            log.error("Error analyzing skateboard trick video", e);
            return fallback();
        }
    }

    private TrickAnalysisResult parseResponse(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);

            SupportedTrick trick = TrickCatalog.fromName(node.path("trick").asText("UNKNOWN"));
            int confidence = node.path("confidence").asInt(0);
            int formScore = node.path("formScore").asInt(0);

            List<String> feedback = new ArrayList<>();
            JsonNode feedbackNode = node.path("feedback");
            if (feedbackNode.isArray()) {
                feedbackNode.forEach(f -> feedback.add(f.asText()));
            }

            List<TrickSequenceEntry> trickSequence = new ArrayList<>();
            JsonNode sequenceNode = node.path("trickSequence");
            if (sequenceNode.isArray()) {
                for (JsonNode entry : sequenceNode) {
                    SupportedTrick seqTrick =
                            TrickCatalog.fromName(entry.path("trick").asText("UNKNOWN"));
                    String timeframe = entry.path("timeframe").asText("");
                    int seqConfidence = entry.path("confidence").asInt(0);
                    trickSequence.add(new TrickSequenceEntry(seqTrick, timeframe, seqConfidence));
                }
            }

            return new TrickAnalysisResult(trick, confidence, formScore, feedback, trickSequence);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", response, e);
            return fallback();
        }
    }

    private String extractJson(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String buildPoseDataText(List<String> base64Frames) {
        try {
            PoseData.SequencePoseData poseData = poseEstimationService.estimatePoses(base64Frames);
            if (poseData == null) {
                return "";
            }
            return """

                    POSE ESTIMATION DATA:
                    Below is structured skeletal pose data from YOLO11n-Pose.
                    Use this to SUPPLEMENT your visual analysis.
                    Key signals:
                    - feet_airborne=true + rotation > 90° = spin/flip trick
                    - feet_airborne=false for ALL frames = manual/cruising
                    - knee < 90° = deep crouch (setup/landing)
                    - body_rotation delta > 90° = 180 trick
                    - board airborne (detected via YOLO object detection) = board left the ground

                    """ + poseData.toPromptText();
        } catch (Exception e) {
            log.warn("Failed to generate pose data for prompt, continuing without it", e);
            return "";
        }
    }

    private TrickAnalysisResult fallback() {
        return new TrickAnalysisResult(
                SupportedTrick.UNKNOWN, 0, 0, List.of("AI model not available. Please try again later."));
    }
}
