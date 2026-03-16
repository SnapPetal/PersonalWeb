package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.api.TrickSequenceEntry;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.AdvisorParams;
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
                    You are an expert skateboarding trick judge. Your output MUST be ONLY a valid JSON object. \
                    Do NOT include any text, reasoning, frame-by-frame analysis, markdown, or explanation — \
                    only the JSON object itself.

                    Internally analyze the sequential frames using these rules, but output ONLY JSON:

                    DETECTION RULES:
                    - Board LEAVES GROUND completely → NOT Manual or Cruising (use OLLIE or flip/spin variant)
                    - Board contacts rail/ledge and slides → GRIND variant
                    - OLLIE: tail pop, board rises, ALL wheels off ground, lands back down
                    - 180 variants: ollie + 180° body/board rotation
                    - KICKFLIP: ollie + board flips along length axis (grip tape flashes)
                    - HEELFLIP: opposite flip direction from kickflip
                    - POP_SHUVIT: board spins 180° flat, no flip
                    - TREFLIP: kickflip + 360 shuvit simultaneously
                    - BOARDSLIDE: board perpendicular across rail/obstacle
                    - FIFTY_FIFTY: both trucks grinding on edge
                    - FIVE_O: back truck only on edge, nose lifted
                    - NOSEGRIND: front truck only on edge, tail lifted
                    - MANUAL: back wheels only, nose lifted, wheels NEVER leave ground
                    - CRUISING: all 4 wheels on flat ground entire clip, no trick
                    - DROP_IN: rider at top of ramp/bowl, transitions down the ramp

                    Look for: setup stance → pop/initiation → airborne phase → catch/landing.

                    MULTIPLE TRICKS: List ALL tricks in trickSequence chronologically. \
                    The "trick" field is the most significant one.

                    Known trick enum values: %s

                    Use ENUM_NAME exactly (e.g., OLLIE, KICKFLIP, POP_SHUVIT, TREFLIP, FRONTSIDE_180, \
                    BACKSIDE_180, FIVE_O, NOSEGRIND, CRUISING, DROP_IN). Use UNKNOWN only if unidentifiable.
                    """.formatted(TrickCatalog.buildTrickDescriptions()) + similarExamples + poseDataText;

            String userPrompt =
                    ("These %d images are sequential frames from a skateboarding video, evenly spaced in time. "
                                    + "Frame 1 is earliest, frame %d is latest. Analyze the full progression of movement across all frames and identify all tricks performed in sequence.")
                            .formatted(base64Frames.size(), base64Frames.size());

            final var schema = callAndExtract(systemPrompt, userPrompt, mediaList.toArray(new Media[0]));

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
                    You are an expert skateboarding trick judge. Your output MUST be ONLY a valid JSON object. \
                    Do NOT include any text, reasoning, frame-by-frame analysis, markdown, or explanation — \
                    only the JSON object itself.

                    Internally analyze the video using these rules, but output ONLY JSON:

                    DETECTION RULES:
                    - Board LEAVES GROUND completely → NOT Manual or Cruising (use OLLIE or flip/spin variant)
                    - Board contacts rail/ledge and slides → GRIND variant
                    - OLLIE: tail pop, board rises, ALL wheels off ground, lands back down
                    - 180 variants: ollie + 180° body/board rotation
                    - KICKFLIP: ollie + board flips along length axis (grip tape flashes)
                    - HEELFLIP: opposite flip direction from kickflip
                    - POP_SHUVIT: board spins 180° flat, no flip
                    - TREFLIP: kickflip + 360 shuvit simultaneously
                    - BOARDSLIDE: board perpendicular across rail/obstacle
                    - FIFTY_FIFTY: both trucks grinding on edge
                    - FIVE_O: back truck only on edge, nose lifted
                    - NOSEGRIND: front truck only on edge, tail lifted
                    - MANUAL: back wheels only, nose lifted, wheels NEVER leave ground
                    - CRUISING: all 4 wheels on flat ground entire clip, no trick
                    - DROP_IN: rider at top of ramp/bowl, transitions down the ramp

                    Look for: setup stance → pop/initiation → airborne phase → catch/landing.

                    MULTIPLE TRICKS: List ALL tricks in trickSequence chronologically. \
                    The "trick" field is the most significant one.

                    Known trick enum values: %s

                    Use ENUM_NAME exactly (e.g., OLLIE, KICKFLIP, POP_SHUVIT, TREFLIP, FRONTSIDE_180, \
                    BACKSIDE_180, FIVE_O, NOSEGRIND, CRUISING, DROP_IN). Use UNKNOWN only if unidentifiable.
                    """.formatted(TrickCatalog.buildTrickDescriptions());

            String userPrompt = "Watch this skateboarding video and identify the trick being performed. "
                    + "Pay close attention to the board movement, especially whether it leaves the ground.";

            final var schema = callAndExtract(systemPrompt, userPrompt, videoMedia);

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

    /**
     * Calls the AI model with native structured output via Bedrock's outputSchema.
     */
    private TrickAnalysisResponseSchema callAndExtract(
            final String systemPrompt, final String userPrompt, final Media... media) throws Exception {
        return chatClient
                .prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .system(systemPrompt)
                .user(u -> u.text(userPrompt).media(media))
                .call()
                .entity(TrickAnalysisResponseSchema.class);
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
