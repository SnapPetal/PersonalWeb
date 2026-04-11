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
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.model.VectorData;

@Component
@Slf4j
class BedrockTrickAnalyzer implements TrickAnalyzer {

    private final ChatClient chatClient;
    private final PoseEstimationService poseEstimationService;
    private final VideoFrameExtractor videoFrameExtractor;
    private final S3VectorsClient s3VectorsClient;
    private final EmbeddingService embeddingService;
    private final SkatetricksObservability observability;

    @Value("${skatetricks.vectorstore.bucket:}")
    private String vectorBucket;

    @Value("${skatetricks.vectorstore.index:}")
    private String vectorIndex;

    @Autowired(required = false)
    BedrockTrickAnalyzer(
            ChatModel chatModel,
            PoseEstimationService poseEstimationService,
            VideoFrameExtractor videoFrameExtractor,
            @org.springframework.lang.Nullable S3VectorsClient s3VectorsClient,
            @org.springframework.lang.Nullable EmbeddingService embeddingService,
            SkatetricksObservability observability) {
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
        this.poseEstimationService = poseEstimationService;
        this.videoFrameExtractor = videoFrameExtractor;
        this.s3VectorsClient = s3VectorsClient;
        this.embeddingService = embeddingService;
        this.observability = observability;
    }

    @Override
    public TrickAnalysisResult analyze(List<String> base64Frames) {
        final var scope = observability.start("analyzer.analyze_frames");
        if (chatClient == null) {
            log.warn("event=analyzer_unavailable reason=chat_model_missing mode=frames");
            observability.incrementStage("analyzer", "fallback", "reason", "chat_model_missing");
            observability.success(scope, "reason", "chat_model_missing", "mode", "frames");
            return fallback();
        }

        try {
            final var result = analyzeFramesInternal(base64Frames);
            observability.success(scope, "trick", result.trick().name(), "mode", "frames");
            return result;

        } catch (Exception e) {
            log.error("event=analyze_frames_failed frameCount={}", base64Frames.size(), e);
            observability.failure(scope, e, "mode", "frames");
            return fallback();
        }
    }

    @Override
    public TrickAnalysisResult analyzeVideo(byte[] mp4VideoData) {
        final var scope = observability.start("analyzer.analyze_video");
        if (chatClient == null) {
            log.warn("event=analyzer_unavailable reason=chat_model_missing mode=video");
            observability.incrementStage("analyzer", "fallback", "reason", "chat_model_missing");
            observability.success(scope, "reason", "chat_model_missing", "mode", "video");
            return fallback();
        }

        try {
            List<String> extractedFrames = videoFrameExtractor.extractBase64Frames(mp4VideoData);
            if (extractedFrames.isEmpty()) {
                log.warn("event=video_analysis_fallback reason=no_frames_extracted");
                observability.incrementStage("analyzer", "fallback", "reason", "no_frames_extracted");
                observability.success(scope, "reason", "no_frames_extracted", "mode", "video");
                return fallback();
            }
            observability.recordFrameCount(extractedFrames.size(), "mode", "video");
            final var result = analyzeFramesInternal(extractedFrames);
            observability.success(scope, "trick", result.trick().name(), "mode", "video");
            return result;

        } catch (Exception e) {
            log.error("event=video_analysis_failed bytes={}", mp4VideoData.length, e);
            observability.failure(scope, e, "mode", "video");
            return fallback();
        }
    }

    private TrickAnalysisResult analyzeFramesInternal(List<String> base64Frames) throws Exception {
        List<Media> mediaList = base64Frames.stream()
                .map(frame -> Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_JPEG)
                        .data(Base64.getDecoder().decode(frame))
                        .build())
                .toList();

        final var poseTexts = buildPoseDataText(base64Frames);
        final var similarExamples = fetchSimilarExamples(poseTexts.embeddingText());
        log.info(
                "event=analyzer_prompt_context frameCount={} posePrompt={} similarExamples={}",
                base64Frames.size(),
                !poseTexts.promptText().isBlank(),
                !similarExamples.isBlank());

        String systemPrompt =
                """
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
                """.formatted(TrickCatalog.buildTrickDescriptions()) + similarExamples + poseTexts.promptText();

        String userPrompt = ("These %d images are sequential frames from a skateboarding video, evenly spaced in time. "
                        + "Frame 1 is earliest, frame %d is latest. Analyze the full progression of movement across all frames and identify all tricks performed in sequence.")
                .formatted(base64Frames.size(), base64Frames.size());

        final var schema = callAndExtract(systemPrompt, userPrompt, mediaList.toArray(new Media[0]));

        return new TrickAnalysisResult(
                TrickCatalog.fromName(schema.trick()),
                schema.confidence(),
                schema.formScore(),
                schema.feedback(),
                schema.trickSequence().stream()
                        .map(e ->
                                new TrickSequenceEntry(TrickCatalog.fromName(e.trick()), e.timeframe(), e.confidence()))
                        .toList(),
                poseTexts.promptText().isBlank() ? null : poseTexts.promptText(),
                poseTexts.embeddingText().isBlank() ? null : poseTexts.embeddingText());
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
            log.warn("event=similar_examples_fetch_failed errorMessage={}", e.getMessage());
            return "";
        }
    }

    private record PoseTexts(String promptText, String embeddingText) {
        static final PoseTexts EMPTY = new PoseTexts("", "");
    }

    private PoseTexts buildPoseDataText(List<String> base64Frames) {
        try {
            PoseData.SequencePoseData poseData = poseEstimationService.estimatePoses(base64Frames);
            if (poseData == null) {
                return PoseTexts.EMPTY;
            }
            final var promptText = """

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
            final var embeddingText = poseData.toEmbeddingText();
            return new PoseTexts(promptText, embeddingText);
        } catch (Exception e) {
            log.warn("event=pose_data_generation_failed fallback=empty", e);
            return PoseTexts.EMPTY;
        }
    }

    private TrickAnalysisResult fallback() {
        return new TrickAnalysisResult(
                SupportedTrick.UNKNOWN, 0, 0, List.of("AI model not available. Please try again later."));
    }
}
