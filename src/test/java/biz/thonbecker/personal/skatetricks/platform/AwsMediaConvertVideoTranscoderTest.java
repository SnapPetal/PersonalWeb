package biz.thonbecker.personal.skatetricks.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.mediaconvert.model.H264CodecLevel;
import software.amazon.awssdk.services.mediaconvert.model.H264CodecProfile;
import software.amazon.awssdk.services.mediaconvert.model.H264RateControlMode;

class AwsMediaConvertVideoTranscoderTest {

    @Test
    void h264SettingsIncludeRequiredQvbrBitrateFields() {
        var settings = AwsMediaConvertVideoTranscoder.buildH264Settings();

        assertEquals(H264RateControlMode.QVBR, settings.rateControlMode());
        assertEquals(H264CodecProfile.MAIN, settings.codecProfile());
        assertEquals(H264CodecLevel.AUTO, settings.codecLevel());
        assertEquals(5_000_000, settings.maxBitrate());
        assertEquals(7, settings.qvbrSettings().qvbrQualityLevel());
    }
}
