package biz.thonbecker.personal.notification.api;

import biz.thonbecker.personal.notification.domain.Notification;
import biz.thonbecker.personal.notification.domain.NotificationChannel;
import java.util.List;

/**
 * Public API for Notification module.
 * This is the only entry point other modules should use to interact with notification
 * functionality.
 */
public interface NotificationFacade {

    /**
     * Send a notification to a user through a specific channel.
     *
     * @param userId the user ID to notify
     * @param title the notification title
     * @param message the notification message
     * @param channel the delivery channel
     * @return the created notification
     */
    Notification sendNotification(String userId, String title, String message, NotificationChannel channel);

    /**
     * Send a notification to multiple users.
     *
     * @param userIds the list of user IDs to notify
     * @param title the notification title
     * @param message the notification message
     * @param channel the delivery channel
     */
    void sendBulkNotification(List<String> userIds, String title, String message, NotificationChannel channel);

    /**
     * Get all notifications for a user.
     *
     * @param userId the user ID
     * @return list of notifications
     */
    List<Notification> getNotificationsForUser(String userId);

    /**
     * Get unread notifications for a user.
     *
     * @param userId the user ID
     * @return list of unread notifications
     */
    List<Notification> getUnreadNotifications(String userId);

    /**
     * Mark a notification as read.
     *
     * @param notificationId the notification ID
     */
    void markAsRead(String notificationId);
}
