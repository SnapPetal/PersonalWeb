package biz.thonbecker.personal.skatetricks.platform;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.Joints;
import ai.djl.modality.cv.translator.YoloPoseTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class PoseEstimationService implements AutoCloseable {

    private static final String[] COCO_KEYPOINTS = {
        "nose",
        "left_eye",
        "right_eye",
        "left_ear",
        "right_ear",
        "left_shoulder",
        "right_shoulder",
        "left_elbow",
        "right_elbow",
        "left_wrist",
        "right_wrist",
        "left_hip",
        "right_hip",
        "left_knee",
        "right_knee",
        "left_ankle",
        "right_ankle"
    };

    // COCO keypoint indices
    private static final int LEFT_SHOULDER = 5;
    private static final int RIGHT_SHOULDER = 6;
    private static final int LEFT_ELBOW = 7;
    private static final int RIGHT_ELBOW = 8;
    private static final int LEFT_WRIST = 9;
    private static final int RIGHT_WRIST = 10;
    private static final int LEFT_HIP = 11;
    private static final int RIGHT_HIP = 12;
    private static final int LEFT_KNEE = 13;
    private static final int RIGHT_KNEE = 14;
    private static final int LEFT_ANKLE = 15;
    private static final int RIGHT_ANKLE = 16;

    private volatile @Nullable ZooModel<Image, Joints[]> model;
    private volatile boolean modelLoadAttempted;

    PoseData.@Nullable SequencePoseData estimatePoses(List<String> base64Frames) {
        ZooModel<Image, Joints[]> loadedModel = ensureModelLoaded();
        if (loadedModel == null) {
            return null;
        }

        try (Predictor<Image, Joints[]> predictor = loadedModel.newPredictor()) {
            List<PoseData.FramePoseData> framePoseDataList = new ArrayList<>();
            List<Double> allBodyRotations = new ArrayList<>();
            List<Double> allAnkleYPositions = new ArrayList<>();
            List<Double> allKneeAngles = new ArrayList<>();

            for (int i = 0; i < base64Frames.size(); i++) {
                Image image = decodeImage(base64Frames.get(i));
                Joints[] results = predictor.predict(image);

                List<PoseData.PersonPose> persons = new ArrayList<>();
                for (Joints joints : results) {
                    PoseData.PersonPose personPose = buildPersonPose(joints);
                    persons.add(personPose);

                    allBodyRotations.add(personPose.bodyRotationAngle());
                    allAnkleYPositions.add(getAverageAnkleY(joints.getJoints()));
                    for (PoseData.JointAngle angle : personPose.angles()) {
                        if (angle.name().contains("knee")) {
                            allKneeAngles.add(angle.angleDegrees());
                        }
                    }
                }

                framePoseDataList.add(new PoseData.FramePoseData(i, persons));
            }

            // Detect airborne by tracking ankle vertical displacement across frames
            int airborneCount = detectAirborneFrames(allAnkleYPositions);

            // Use smoothed cumulative rotation instead of noisy frame-to-frame max
            double maxRotationDelta = computeSmoothedRotationDelta(allBodyRotations);

            double avgKneeAngle = allKneeAngles.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(180.0);
            String motionSummary =
                    buildMotionSummary(airborneCount, base64Frames.size(), maxRotationDelta, avgKneeAngle);

            // Update the feetAirborne flags on PersonPose using cross-frame detection
            List<PoseData.FramePoseData> updatedFrames = applyAirborneDetection(framePoseDataList, allAnkleYPositions);

            PoseData.SequencePoseData result = new PoseData.SequencePoseData(
                    updatedFrames, maxRotationDelta, airborneCount, avgKneeAngle, motionSummary);

            log.info(
                    "Pose estimation complete: {} airborne frames, {} deg max rotation",
                    airborneCount,
                    String.format("%.0f", maxRotationDelta));

            return result;

        } catch (Exception e) {
            log.error("Pose estimation failed, continuing without pose data", e);
            return null;
        }
    }

    private @Nullable ZooModel<Image, Joints[]> ensureModelLoaded() {
        if (model != null) {
            return model;
        }
        if (modelLoadAttempted) {
            return null;
        }

        synchronized (this) {
            if (model != null) {
                return model;
            }
            if (modelLoadAttempted) {
                return null;
            }

            try {
                log.info("Loading YOLO11n-Pose model...");
                Criteria<Image, Joints[]> criteria = Criteria.builder()
                        .setTypes(Image.class, Joints[].class)
                        .optModelUrls("djl://ai.djl.pytorch/yolo11n-pose")
                        .optTranslatorFactory(new YoloPoseTranslatorFactory())
                        .build();
                model = criteria.loadModel();
                log.info("YOLO11n-Pose model loaded successfully");
                return model;
            } catch (Exception e) {
                log.warn("Failed to load YOLO11n-Pose model, pose estimation will be unavailable", e);
                modelLoadAttempted = true;
                return null;
            }
        }
    }

    private Image decodeImage(String base64Frame) throws java.io.IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64Frame);
        return ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
    }

    private PoseData.PersonPose buildPersonPose(Joints joints) {
        List<Joints.Joint> jointList = joints.getJoints();
        List<PoseData.Keypoint> keypoints = new ArrayList<>();

        for (int i = 0; i < jointList.size() && i < COCO_KEYPOINTS.length; i++) {
            Joints.Joint j = jointList.get(i);
            keypoints.add(new PoseData.Keypoint(COCO_KEYPOINTS[i], j.getX(), j.getY(), j.getConfidence()));
        }

        List<PoseData.JointAngle> angles = calculateAngles(jointList);
        double bodyRotation = calculateBodyRotation(jointList);
        // Airborne detection is done cross-frame in applyAirborneDetection; default false here
        boolean feetAirborne = false;
        double[] cog = calculateCenterOfGravity(jointList);

        return new PoseData.PersonPose(keypoints, angles, bodyRotation, feetAirborne, cog[0], cog[1]);
    }

    private List<PoseData.JointAngle> calculateAngles(List<Joints.Joint> joints) {
        List<PoseData.JointAngle> angles = new ArrayList<>();

        if (joints.size() > RIGHT_ANKLE) {
            addAngleIfConfident(angles, "left_knee", joints, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE);
            addAngleIfConfident(angles, "right_knee", joints, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE);
            addAngleIfConfident(angles, "left_hip", joints, LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE);
            addAngleIfConfident(angles, "right_hip", joints, RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE);
            addAngleIfConfident(angles, "left_elbow", joints, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST);
            addAngleIfConfident(angles, "right_elbow", joints, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST);
        }

        return angles;
    }

    private void addAngleIfConfident(
            List<PoseData.JointAngle> angles, String name, List<Joints.Joint> joints, int a, int b, int c) {
        Joints.Joint ja = joints.get(a);
        Joints.Joint jb = joints.get(b);
        Joints.Joint jc = joints.get(c);

        if (ja.getConfidence() > 0.3 && jb.getConfidence() > 0.3 && jc.getConfidence() > 0.3) {
            double angle = calculateAngle(ja.getX(), ja.getY(), jb.getX(), jb.getY(), jc.getX(), jc.getY());
            angles.add(new PoseData.JointAngle(name, angle));
        }
    }

    private double calculateAngle(double ax, double ay, double bx, double by, double cx, double cy) {
        double baX = ax - bx;
        double baY = ay - by;
        double bcX = cx - bx;
        double bcY = cy - by;

        double dot = baX * bcX + baY * bcY;
        double magBA = Math.sqrt(baX * baX + baY * baY);
        double magBC = Math.sqrt(bcX * bcX + bcY * bcY);

        if (magBA == 0 || magBC == 0) {
            return 180.0;
        }

        double cosAngle = Math.max(-1.0, Math.min(1.0, dot / (magBA * magBC)));
        return Math.toDegrees(Math.acos(cosAngle));
    }

    private double calculateBodyRotation(List<Joints.Joint> joints) {
        if (joints.size() <= RIGHT_SHOULDER) {
            return Double.NaN;
        }

        Joints.Joint leftShoulder = joints.get(LEFT_SHOULDER);
        Joints.Joint rightShoulder = joints.get(RIGHT_SHOULDER);

        // Require higher confidence to avoid noisy detections causing false rotation
        if (leftShoulder.getConfidence() < 0.5 || rightShoulder.getConfidence() < 0.5) {
            return Double.NaN;
        }

        double dx = rightShoulder.getX() - leftShoulder.getX();
        double dy = rightShoulder.getY() - leftShoulder.getY();
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private double getAverageAnkleY(List<Joints.Joint> joints) {
        if (joints.size() <= RIGHT_ANKLE) {
            return Double.NaN;
        }

        Joints.Joint leftAnkle = joints.get(LEFT_ANKLE);
        Joints.Joint rightAnkle = joints.get(RIGHT_ANKLE);

        boolean leftValid = leftAnkle.getConfidence() > 0.3;
        boolean rightValid = rightAnkle.getConfidence() > 0.3;

        if (leftValid && rightValid) {
            return (leftAnkle.getY() + rightAnkle.getY()) / 2.0;
        } else if (leftValid) {
            return leftAnkle.getY();
        } else if (rightValid) {
            return rightAnkle.getY();
        }
        return Double.NaN;
    }

    private int detectAirborneFrames(List<Double> ankleYPositions) {
        // Establish baseline from first few valid frames (grounded position)
        List<Double> validPositions =
                ankleYPositions.stream().filter(y -> !Double.isNaN(y)).toList();

        if (validPositions.size() < 3) {
            return 0;
        }

        // Baseline = average ankle Y from first 20% of frames (assumed grounded setup)
        int baselineCount = Math.max(2, validPositions.size() / 5);
        double baseline = validPositions.stream()
                .limit(baselineCount)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // A frame is "airborne" if ankles moved upward (lower Y) by > 5% of image height
        // relative to baseline. This catches whole-body displacement.
        double threshold = 0.05;
        int airborneCount = 0;
        for (Double ankleY : ankleYPositions) {
            if (!Double.isNaN(ankleY) && (baseline - ankleY) > threshold) {
                airborneCount++;
            }
        }
        return airborneCount;
    }

    private List<PoseData.FramePoseData> applyAirborneDetection(
            List<PoseData.FramePoseData> frames, List<Double> ankleYPositions) {
        List<Double> validPositions =
                ankleYPositions.stream().filter(y -> !Double.isNaN(y)).toList();
        if (validPositions.size() < 3) {
            return frames;
        }

        int baselineCount = Math.max(2, validPositions.size() / 5);
        double baseline = validPositions.stream()
                .limit(baselineCount)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double threshold = 0.05;

        List<PoseData.FramePoseData> updated = new ArrayList<>();
        int ankleIdx = 0;
        for (PoseData.FramePoseData frame : frames) {
            List<PoseData.PersonPose> updatedPersons = new ArrayList<>();
            for (PoseData.PersonPose person : frame.persons()) {
                boolean airborne = false;
                if (ankleIdx < ankleYPositions.size()) {
                    double ankleY = ankleYPositions.get(ankleIdx);
                    airborne = !Double.isNaN(ankleY) && (baseline - ankleY) > threshold;
                    ankleIdx++;
                }
                updatedPersons.add(new PoseData.PersonPose(
                        person.keypoints(),
                        person.angles(),
                        person.bodyRotationAngle(),
                        airborne,
                        person.cogX(),
                        person.cogY()));
            }
            // If frame had no persons, still advance
            if (frame.persons().isEmpty() && ankleIdx < ankleYPositions.size()) {
                ankleIdx++;
            }
            updated.add(new PoseData.FramePoseData(frame.frameIndex(), updatedPersons));
        }
        return updated;
    }

    private double[] calculateCenterOfGravity(List<Joints.Joint> joints) {
        double sumX = 0;
        double sumY = 0;
        int count = 0;

        // Use major body joints for center of gravity
        int[] cogJoints = {LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HIP, RIGHT_HIP};
        for (int idx : cogJoints) {
            if (idx < joints.size() && joints.get(idx).getConfidence() > 0.3) {
                sumX += joints.get(idx).getX();
                sumY += joints.get(idx).getY();
                count++;
            }
        }

        if (count == 0) {
            return new double[] {0.5, 0.5};
        }
        return new double[] {sumX / count, sumY / count};
    }

    private double computeSmoothedRotationDelta(List<Double> rotations) {
        // Filter out NaN values (low-confidence frames)
        List<Double> valid = rotations.stream().filter(r -> !Double.isNaN(r)).toList();

        if (valid.size() < 3) {
            return 0.0;
        }

        // Use a 3-frame sliding window median to smooth jitter,
        // then compute max delta between smoothed values
        List<Double> smoothed = new ArrayList<>();
        for (int i = 0; i < valid.size(); i++) {
            int start = Math.max(0, i - 1);
            int end = Math.min(valid.size() - 1, i + 1);
            List<Double> window = new ArrayList<>(valid.subList(start, end + 1));
            window.sort(Double::compareTo);
            smoothed.add(window.get(window.size() / 2));
        }

        double maxDelta = 0.0;
        for (int i = 1; i < smoothed.size(); i++) {
            double delta = Math.abs(smoothed.get(i) - smoothed.get(i - 1));
            if (delta > 180) {
                delta = 360 - delta;
            }
            maxDelta = Math.max(maxDelta, delta);
        }
        return maxDelta;
    }

    private String buildMotionSummary(
            int airborneFrames, int totalFrames, double maxRotationDelta, double avgKneeAngle) {
        List<String> observations = new ArrayList<>();

        double airborneRatio = (double) airborneFrames / totalFrames;
        if (airborneRatio > 0.3) {
            observations.add("airborne");
        } else if (airborneRatio > 0.1) {
            observations.add("briefly airborne");
        } else {
            observations.add("grounded");
        }

        if (maxRotationDelta > 90) {
            observations.add("significant rotation");
        } else if (maxRotationDelta > 30) {
            observations.add("moderate rotation");
        }

        if (avgKneeAngle < 90) {
            observations.add("deep crouch detected");
        } else if (avgKneeAngle < 130) {
            observations.add("bent knees");
        }

        return String.join(" with ", observations);
    }

    @Override
    public void close() {
        ZooModel<Image, Joints[]> m = model;
        if (m != null) {
            m.close();
            model = null;
        }
    }
}
