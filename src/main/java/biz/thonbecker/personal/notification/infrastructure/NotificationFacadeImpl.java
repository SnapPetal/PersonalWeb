package biz.thonbecker.personal.notification.infrastructure;

import biz.thonbecker.personal.notification.api.NotificationFacade;
import biz.thonbecker.personal.notification.api.NotificationSentEvent;
import biz.thonbecker.personal.notification.domain.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
class NotificationFacadeImpl implements NotificationFacade {

    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();
    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    public NotificationFacadeImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Notification sendNotification(
            String userId, String title, String message, NotificationChannel channel) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setChannel(channel);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setCreatedAt(Instant.now());
        notification.setSentAt(Instant.now());
        notification.setStatus(NotificationStatus.SENT);

        // Store notification
        notifications.put(notification.getId(), notification);
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);

        log.info("Notification sent to user {} via {}: {}", userId, channel, title);

        // Publish event
        eventPublisher.publishEvent(new NotificationSentEvent(
                notification.getId(), userId, title, channel, notification.getSentAt()));

        return notification;
    }

    @Override
    public void sendBulkNotification(
            List<String> userIds, String title, String message, NotificationChannel channel) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("No user IDs provided for bulk notification");
            return;
        }

        log.info("Sending bulk notification to {} users via {}", userIds.size(), channel);
        userIds.forEach(userId -> sendNotification(userId, title, message, channel));
    }

    @Override
    public List<Notification> getNotificationsForUser(String userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>());
    }

    @Override
    public List<Notification> getUnreadNotifications(String userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>()).stream()
                .filter(n -> n.getStatus() == NotificationStatus.SENT)
                .toList();
    }

    @Override
    public void markAsRead(String notificationId) {
        Notification notification = notifications.get(notificationId);
        if (notification != null) {
            notification.setStatus(NotificationStatus.DELIVERED);
            log.debug("Notification {} marked as read", notificationId);
        }
    }
}
