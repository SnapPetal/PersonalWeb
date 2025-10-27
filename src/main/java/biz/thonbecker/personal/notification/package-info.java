/**
 * Notification Module
 *
 * <p>This module handles sending notifications to users across different channels.
 * It listens to events from other modules and sends appropriate notifications.
 *
 * <p>Public API:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.notification.api.NotificationFacade} - Main operations for
 *       notifications</li>
 *   <li>{@link biz.thonbecker.personal.notification.domain.Notification} - Notification domain
 *       model</li>
 *   <li>{@link biz.thonbecker.personal.notification.domain.NotificationChannel} - Delivery
 *       channels</li>
 *   <li>{@link biz.thonbecker.personal.notification.api.NotificationSentEvent} - Published when
 *       notification is sent</li>
 * </ul>
 *
 * <p>Module Dependencies:
 * <ul>
 *   <li>shared - For infrastructure services</li>
 *   <li>user - For user profile and preferences</li>
 * </ul>
 *
 * <p>Event Subscriptions:
 * <ul>
 *   <li>QuizCompletedEvent - Notify winner and participants</li>
 *   <li>QuizStartedEvent - Notify participants quiz has started</li>
 *   <li>GameRecordedEvent - Notify players of foosball results</li>
 *   <li>UserRegisteredEvent - Send welcome notification</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification Services",
        allowedDependencies = {"shared", "user"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.springframework.lang.NonNullApi
package biz.thonbecker.personal.notification;
