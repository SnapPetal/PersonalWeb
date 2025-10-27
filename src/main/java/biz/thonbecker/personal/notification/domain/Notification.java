package biz.thonbecker.personal.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Notification domain model representing a message to be sent to a user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private Instant createdAt;
    private Instant sentAt;
    private NotificationStatus status;
}
