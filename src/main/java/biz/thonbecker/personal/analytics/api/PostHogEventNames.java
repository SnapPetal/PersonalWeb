package biz.thonbecker.personal.analytics.api;

/** Canonical PostHog event names used by application funnels. */
public final class PostHogEventNames {

    public static final String BOOKING_STARTED = "booking_started";
    public static final String BOOKING_AVAILABILITY_VIEWED = "booking_availability_viewed";
    public static final String BOOKING_SUBMITTED = "booking_submitted";
    public static final String BOOKING_CONFIRMED = "booking_confirmed";
    public static final String BOOKING_CANCELLED = "booking_cancelled";
    public static final String LANDSCAPE_UPLOAD_STARTED = "landscape_upload_started";
    public static final String LANDSCAPE_ANALYSIS_COMPLETED = "landscape_analysis_completed";
    public static final String LANDSCAPE_PLANT_ADDED = "landscape_plant_added";
    public static final String LANDSCAPE_PLAN_SAVED = "landscape_plan_saved";
    public static final String LANDSCAPE_PREVIEW_GENERATED = "landscape_preview_generated";
    public static final String SKATETRICKS_ANALYSIS_STARTED = "skatetricks_analysis_started";
    public static final String SKATETRICKS_ANALYSIS_COMPLETED = "skatetricks_analysis_completed";
    public static final String SKATETRICKS_ATTEMPT_VERIFIED = "skatetricks_attempt_verified";
    public static final String TRIVIA_JOINED = "trivia_joined";
    public static final String TRIVIA_STARTED = "trivia_started";
    public static final String TRIVIA_ANSWER_SUBMITTED = "trivia_answer_submitted";
    public static final String TRIVIA_COMPLETED = "trivia_completed";
    public static final String AUTH_LOGIN_REQUESTED = "auth_login_requested";
    public static final String AUTH_LOGIN_COMPLETED = "auth_login_completed";
    public static final String AUTH_LOGIN_FAILED = "auth_login_failed";

    private PostHogEventNames() {}
}
