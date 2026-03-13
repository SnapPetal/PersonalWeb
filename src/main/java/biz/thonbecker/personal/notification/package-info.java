/**
 * Notification Module
 *
 * <p>This module handles sending notifications to users across different channels.
 * It is fully event-driven - listening to events from other modules and sending
 * appropriate notifications via email, SMS, push, etc.
 *
 * <p>This module has no public API facade. All communication happens through event listeners.
 * Other modules should publish events, not call notification services directly.
 *
 * <p>Module Dependencies:
 * <ul>
 *   <li>shared - For infrastructure services and booking domain events</li>
 *   <li>trivia - For quiz-related event types only (no method calls)</li>
 *   <li>foosball - For game-related event types only (no method calls)</li>
 *   <li>user - For user-related event types only (no method calls)</li>
 * </ul>
 *
 * <p>Event Subscriptions:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.shared.events.BookingCreatedEvent} - Send booking confirmation email to attendee and admin</li>
 *   <li>{@link biz.thonbecker.personal.shared.events.BookingCancelledEvent} - Send cancellation notification to attendee</li>
 *   <li>QuizCompletedEvent, QuizStartedEvent, PlayerJoinedQuizEvent - Log quiz events</li>
 *   <li>GameRecordedEvent, PlayerCreatedEvent - Log foosball events</li>
 *   <li>UserRegisteredEvent, UserLoginEvent, UserProfileUpdatedEvent - Log user events</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification Services",
        allowedDependencies = {"shared", "trivia", "foosball", "user"})
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.notification;
