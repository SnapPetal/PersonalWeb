package biz.thonbecker.personal.notification.domain;

/**
 * Status of a notification delivery.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED
}
