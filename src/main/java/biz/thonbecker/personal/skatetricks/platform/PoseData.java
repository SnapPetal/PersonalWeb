package biz.thonbecker.personal.skatetricks.platform;

import java.util.List;
import java.util.stream.Collectors;

final class PoseData {

    private PoseData() {}

    record Keypoint(String name, double x, double y, double confidence) {
        String toPromptText() {
            if (confidence < 0.3) {
                return name + "=low_conf";
            }
            return "%s=(%.2f,%.2f)".formatted(name, x, y);
        }
    }

    record BoardDetection(boolean detected, boolean airborne, double bottomY, double confidence) {
        String toPromptText() {
            if (!detected) {
                return "board=not_detected";
            }
            return "board=(detected, airborne=%s, bottomY=%.2f, conf=%.2f)".formatted(airborne, bottomY, confidence);
        }
    }

    record JointAngle(String name, double angleDegrees) {
        String toPromptText() {
            return "%s=%.0f°".formatted(name, angleDegrees);
        }
    }

    record PersonPose(
            List<Keypoint> keypoints,
            List<JointAngle> angles,
            double bodyRotationAngle,
            boolean feetAirborne,
            double cogX,
            double cogY) {

        String toPromptText() {
            String angleText = angles.stream().map(JointAngle::toPromptText).collect(Collectors.joining(", "));
            return "person{angles=[%s], body_rotation=%.0f°, feet_airborne=%s, cog=(%.2f,%.2f)}"
                    .formatted(angleText, bodyRotationAngle, feetAirborne, cogX, cogY);
        }
    }

    record FramePoseData(int frameIndex, List<PersonPose> persons, BoardDetection boardDetection) {
        FramePoseData(int frameIndex, List<PersonPose> persons) {
            this(frameIndex, persons, null);
        }

        String toPromptText() {
            if (persons.isEmpty()) {
                return "frame_%d: no_person_detected".formatted(frameIndex);
            }
            String personText = persons.stream().map(PersonPose::toPromptText).collect(Collectors.joining("; "));
            String boardText = boardDetection != null ? " | " + boardDetection.toPromptText() : "";
            return "frame_%d: %s%s".formatted(frameIndex, personText, boardText);
        }
    }

    record SequencePoseData(
            List<FramePoseData> frames,
            double maxBodyRotationDelta,
            int framesWithFeetAirborne,
            int framesWithBoardAirborne,
            double averageKneeAngle,
            String motionSummary) {

        SequencePoseData(
                List<FramePoseData> frames,
                double maxBodyRotationDelta,
                int framesWithFeetAirborne,
                double averageKneeAngle,
                String motionSummary) {
            this(frames, maxBodyRotationDelta, framesWithFeetAirborne, 0, averageKneeAngle, motionSummary);
        }

        String toPromptText() {
            StringBuilder sb = new StringBuilder();
            sb.append("SEQUENCE SUMMARY: ").append(motionSummary).append("\n");
            sb.append(
                    "max_rotation_delta=%.0f°, airborne_frames=%d/%d, board_airborne_frames=%d/%d, avg_knee_angle=%.0f°\n"
                            .formatted(
                                    maxBodyRotationDelta,
                                    framesWithFeetAirborne,
                                    frames.size(),
                                    framesWithBoardAirborne,
                                    frames.size(),
                                    averageKneeAngle));
            sb.append("\nPER-FRAME DATA:\n");
            for (FramePoseData frame : frames) {
                sb.append(frame.toPromptText()).append("\n");
            }
            return sb.toString();
        }

        /**
         * Produces a compact, semantically rich summary optimized for text embedding.
         * Unlike toPromptText() which dumps raw per-frame data for Claude's visual analysis,
         * this captures the motion signature in a format that produces meaningful vector
         * similarity — similar tricks will have similar embedding text.
         */
        String toEmbeddingText() {
            final var sb = new StringBuilder();
            final var totalFrames = frames.size();
            final var airborneRatio = totalFrames > 0 ? (double) framesWithFeetAirborne / totalFrames : 0;
            final var boardAirborneRatio = totalFrames > 0 ? (double) framesWithBoardAirborne / totalFrames : 0;

            // Motion classification
            sb.append("motion: ").append(motionSummary).append("\n");
            sb.append("rotation: %.0f degrees\n".formatted(maxBodyRotationDelta));
            sb.append("airborne: %.0f%% of frames, board_airborne: %.0f%% of frames\n"
                    .formatted(airborneRatio * 100, boardAirborneRatio * 100));
            sb.append("knee_bend: %.0f degrees average\n".formatted(averageKneeAngle));
            sb.append("total_frames: %d\n".formatted(totalFrames));

            // Sample key frames: first, 25%, 50%, 75%, last
            final int[] sampleIndices = totalFrames <= 5
                    ? java.util.stream.IntStream.range(0, totalFrames).toArray()
                    : new int[] {0, totalFrames / 4, totalFrames / 2, 3 * totalFrames / 4, totalFrames - 1};

            sb.append("\nkey_frames:\n");
            for (final var idx : sampleIndices) {
                if (idx < frames.size()) {
                    final var frame = frames.get(idx);
                    final var phase = describePhase(idx, totalFrames);
                    sb.append("  %s (frame %d): ".formatted(phase, frame.frameIndex()));
                    if (!frame.persons().isEmpty()) {
                        final var person = frame.persons().get(0);
                        sb.append("rotation=%.0f° ".formatted(person.bodyRotationAngle()));
                        sb.append("airborne=%s ".formatted(person.feetAirborne()));
                        final var angles = person.angles().stream()
                                .map(a -> "%s=%.0f°".formatted(a.name(), a.angleDegrees()))
                                .collect(Collectors.joining(", "));
                        sb.append("angles=[%s]".formatted(angles));
                    } else {
                        sb.append("no_person");
                    }
                    if (frame.boardDetection() != null) {
                        sb.append(" board=%s".formatted(frame.boardDetection().airborne() ? "airborne" : "grounded"));
                    }
                    sb.append("\n");
                }
            }

            return sb.toString();
        }

        private static String describePhase(final int frameIndex, final int totalFrames) {
            final var ratio = (double) frameIndex / Math.max(totalFrames - 1, 1);
            if (ratio < 0.15) return "setup";
            if (ratio < 0.35) return "launch";
            if (ratio < 0.65) return "peak";
            if (ratio < 0.85) return "descent";
            return "landing";
        }
    }
}
