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
            int airborneCount = 0;
            List<Double> allKneeAngles = new ArrayList<>();

            for (int i = 0; i < base64Frames.size(); i++) {
                Image image = decodeImage(base64Frames.get(i));
                Joints[] results = predictor.predict(image);

                List<PoseData.PersonPose> persons = new ArrayList<>();
                for (Joints joints : results) {
                    PoseData.PersonPose personPose = buildPersonPose(joints);
                    persons.add(personPose);

                    allBodyRotations.add(personPose.bodyRotationAngle());
                    if (personPose.feetAirborne()) {
                        airborneCount++;
                    }
                    for (PoseData.JointAngle angle : personPose.angles()) {
                        if (angle.name().contains("knee")) {
                            allKneeAngles.add(angle.angleDegrees());
                        }
                    }
                }

                framePoseDataList.add(new PoseData.FramePoseData(i, persons));
            }

            double maxRotationDelta = computeMaxRotationDelta(allBodyRotations);
            double avgKneeAngle = allKneeAngles.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(180.0);
            String motionSummary =
                    buildMotionSummary(airborneCount, base64Frames.size(), maxRotationDelta, avgKneeAngle);

            PoseData.SequencePoseData result = new PoseData.SequencePoseData(
                    framePoseDataList, maxRotationDelta, airborneCount, avgKneeAngle, motionSummary);

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
        boolean feetAirborne = detectFeetAirborne(jointList);
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
            return 0.0;
        }

        Joints.Joint leftShoulder = joints.get(LEFT_SHOULDER);
        Joints.Joint rightShoulder = joints.get(RIGHT_SHOULDER);

        if (leftShoulder.getConfidence() < 0.3 || rightShoulder.getConfidence() < 0.3) {
            return 0.0;
        }

        double dx = rightShoulder.getX() - leftShoulder.getX();
        double dy = rightShoulder.getY() - leftShoulder.getY();
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private boolean detectFeetAirborne(List<Joints.Joint> joints) {
        if (joints.size() <= RIGHT_ANKLE) {
            return false;
        }

        Joints.Joint leftAnkle = joints.get(LEFT_ANKLE);
        Joints.Joint rightAnkle = joints.get(RIGHT_ANKLE);
        Joints.Joint leftKnee = joints.get(LEFT_KNEE);
        Joints.Joint rightKnee = joints.get(RIGHT_KNEE);

        if (leftAnkle.getConfidence() < 0.3
                || rightAnkle.getConfidence() < 0.3
                || leftKnee.getConfidence() < 0.3
                || rightKnee.getConfidence() < 0.3) {
            return false;
        }

        // In image coordinates, Y increases downward.
        // If ankles are significantly above (lower Y) where they'd normally be relative to knees,
        // the feet are likely airborne. Normal standing: ankle Y > knee Y.
        // Airborne: ankle Y approaches or goes above knee Y.
        double leftAnkleKneeDiff = leftAnkle.getY() - leftKnee.getY();
        double rightAnkleKneeDiff = rightAnkle.getY() - rightKnee.getY();

        // When standing, ankle is well below knee (diff > ~0.05).
        // When airborne/tucked, this difference shrinks significantly.
        double threshold = 0.03;
        return leftAnkleKneeDiff < threshold && rightAnkleKneeDiff < threshold;
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

    private double computeMaxRotationDelta(List<Double> rotations) {
        if (rotations.size() < 2) {
            return 0.0;
        }

        double maxDelta = 0.0;
        for (int i = 1; i < rotations.size(); i++) {
            double delta = Math.abs(rotations.get(i) - rotations.get(i - 1));
            // Handle angle wrapping
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
