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

    record FramePoseData(int frameIndex, List<PersonPose> persons) {
        String toPromptText() {
            if (persons.isEmpty()) {
                return "frame_%d: no_person_detected".formatted(frameIndex);
            }
            String personText = persons.stream().map(PersonPose::toPromptText).collect(Collectors.joining("; "));
            return "frame_%d: %s".formatted(frameIndex, personText);
        }
    }

    record SequencePoseData(
            List<FramePoseData> frames,
            double maxBodyRotationDelta,
            int framesWithFeetAirborne,
            double averageKneeAngle,
            String motionSummary) {

        String toPromptText() {
            StringBuilder sb = new StringBuilder();
            sb.append("SEQUENCE SUMMARY: ").append(motionSummary).append("\n");
            sb.append("max_rotation_delta=%.0f°, airborne_frames=%d/%d, avg_knee_angle=%.0f°\n"
                    .formatted(maxBodyRotationDelta, framesWithFeetAirborne, frames.size(), averageKneeAngle));
            sb.append("\nPER-FRAME DATA:\n");
            for (FramePoseData frame : frames) {
                sb.append(frame.toPromptText()).append("\n");
            }
            return sb.toString();
        }
    }
}
