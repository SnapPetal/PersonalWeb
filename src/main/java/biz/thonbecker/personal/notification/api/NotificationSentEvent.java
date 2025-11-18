package biz.thonbecker.personal.notification.api;

import biz.thonbecker.personal.notification.domain.NotificationChannel;
import java.time.Instant;

/**
 * Event published when a notification is sent.
 *
 * @param notificationId the notification ID
 * @param userId the user ID who received the notification
 * @param title the notification title
 * @param channel the delivery channel used
 * @param sentAt the timestamp when the notification was sent
 */
public record NotificationSentEvent(
        String notificationId, String userId, String title, NotificationChannel channel, Instant sentAt) {}
