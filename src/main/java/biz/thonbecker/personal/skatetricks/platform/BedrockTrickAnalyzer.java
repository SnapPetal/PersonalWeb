package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.api.TrickSequenceEntry;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.model.VectorData;

@Component
@Slf4j
class BedrockTrickAnalyzer implements TrickAnalyzer {

    private final ChatClient chatClient;
    private final PoseEstimationService poseEstimationService;
    private final S3VectorsClient s3VectorsClient;
    private final EmbeddingService embeddingService;

    @Value("${skatetricks.vectorstore.bucket}")
    private String vectorBucket;

    @Value("${skatetricks.vectorstore.index}")
    private String vectorIndex;

    @Autowired(required = false)
    BedrockTrickAnalyzer(
            ChatModel chatModel,
            PoseEstimationService poseEstimationService,
            S3VectorsClient s3VectorsClient,
            EmbeddingService embeddingService) {
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
        this.poseEstimationService = poseEstimationService;
        this.s3VectorsClient = s3VectorsClient;
        this.embeddingService = embeddingService;
    }

    @Override
    public TrickAnalysisResult analyze(List<String> base64Frames) {
        if (chatClient == null) {
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

            final var poseDataText = buildPoseDataText(base64Frames);
            final var similarExamples = fetchSimilarExamples(poseDataText);

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
                    - Drop In: rider starts at TOP of ramp/halfpipe/bowl with board on coping edge, then leans forward and rides DOWN the transition. Look for: curved ramp surface, rider going from horizontal to angled downward, coping/edge at top of ramp

                    Known trick enum values and descriptions:
                    %s

                    The "trick" field is the primary/most significant trick. The "trickSequence" lists ALL tricks in chronological order.
                    If only one trick is visible, trickSequence should contain just that one entry.
                    Use the ENUM_NAME exactly as listed above (e.g., OLLIE, KICKFLIP, POP_SHUVIT, TREFLIP, FRONTSIDE_180, BACKSIDE_180, FIVE_O, NOSEGRIND, CRUISING, DROP_IN). Use UNKNOWN only if you truly cannot identify the trick.
                    """.formatted(TrickCatalog.buildTrickDescriptions()) + similarExamples + poseDataText;

            String userPrompt =
                    ("These %d images are sequential frames from a skateboarding video, evenly spaced in time. "
                                    + "Frame 1 is earliest, frame %d is latest. Analyze the full progression of movement across all frames and identify all tricks performed in sequence.")
                            .formatted(base64Frames.size(), base64Frames.size());

            final var schema = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt).media(mediaList.toArray(new Media[0])))
                    .call()
                    .entity(TrickAnalysisResponseSchema.class);

            return new TrickAnalysisResult(
                    TrickCatalog.fromName(schema.trick()),
                    schema.confidence(),
                    schema.formScore(),
                    schema.feedback(),
                    schema.trickSequence().stream()
                            .map(e -> new TrickSequenceEntry(
                                    TrickCatalog.fromName(e.trick()), e.timeframe(), e.confidence()))
                            .toList(),
                    poseDataText.isBlank() ? null : poseDataText);

        } catch (Exception e) {
            log.error("Error analyzing skateboard trick frames", e);
            return fallback();
        }
    }

    @Override
    public TrickAnalysisResult analyzeVideo(byte[] mp4VideoData) {
        if (chatClient == null) {
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

                    The "trick" field is the primary/most significant trick. The "trickSequence" lists ALL tricks in chronological order.
                    If only one trick is visible, trickSequence should contain just that one entry.
                    Use the ENUM_NAME exactly as listed above (e.g., OLLIE, KICKFLIP, POP_SHUVIT, TREFLIP, FRONTSIDE_180, BACKSIDE_180, FIVE_O, NOSEGRIND, CRUISING). Use UNKNOWN only if you truly cannot identify the trick.
                    """.formatted(TrickCatalog.buildTrickDescriptions());

            String userPrompt = "Watch this skateboarding video and identify the trick being performed. "
                    + "Pay close attention to the board movement, especially whether it leaves the ground.";

            final var schema = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt).media(videoMedia))
                    .call()
                    .entity(TrickAnalysisResponseSchema.class);

            return new TrickAnalysisResult(
                    TrickCatalog.fromName(schema.trick()),
                    schema.confidence(),
                    schema.formScore(),
                    schema.feedback(),
                    schema.trickSequence().stream()
                            .map(e -> new TrickSequenceEntry(
                                    TrickCatalog.fromName(e.trick()), e.timeframe(), e.confidence()))
                            .toList());

        } catch (Exception e) {
            log.error("Error analyzing skateboard trick video", e);
            return fallback();
        }
    }

    private String fetchSimilarExamples(final String poseText) {
        if (Objects.isNull(s3VectorsClient) || Objects.isNull(embeddingService) || poseText.isBlank()) {
            return "";
        }
        try {
            final var queryEmbedding = embeddingService.embed(poseText);
            final var response = s3VectorsClient.queryVectors(r -> r.vectorBucketName(vectorBucket)
                    .indexName(vectorIndex)
                    .queryVector(VectorData.fromFloat32(queryEmbedding))
                    .topK(3)
                    .returnMetadata(true)
                    .returnDistance(false));

            if (response.vectors().isEmpty()) {
                return "";
            }

            final var sb = new StringBuilder("\nSIMILAR VERIFIED PAST ATTEMPTS (use as reference examples):\n");
            response.vectors().forEach(match -> {
                final var meta = match.metadata().asMap();
                final var trickName = meta.get("trickName").asString();
                final var confidence = meta.get("confidence").asNumber().intValue();
                final var formScore = meta.get("formScore").asNumber().intValue();
                final var feedback =
                        meta.containsKey("feedback") ? meta.get("feedback").asString() : "";
                sb.append("- ")
                        .append(trickName)
                        .append(" (verified confidence: ")
                        .append(confidence)
                        .append("%, form score: ")
                        .append(formScore)
                        .append("%)");
                if (!feedback.isBlank()) {
                    sb.append(" feedback: ").append(feedback);
                }
                sb.append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not fetch similar examples from vector store: {}", e.getMessage());
            return "";
        }
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
