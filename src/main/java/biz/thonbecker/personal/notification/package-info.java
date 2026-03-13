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
 * <p><strong>Architecture Note (Spring Modulith Pattern):</strong><br>
 * This module depends on other modules' APIs to import their event types (e.g., {@code booking.api.BookingCreatedEvent}).
 * This is the correct Spring Modulith pattern: events are part of a module's "Provided Interface" and should
 * live in the publishing module's API package. Event subscribers naturally depend on publishers' APIs to
 * import event type definitions, but maintain zero runtime coupling through asynchronous event handling.
 *
 * <p>Module Dependencies (Event Type Imports Only):
 * <ul>
 *   <li>shared - For infrastructure services (no event dependencies)</li>
 *   <li>booking - Imports {@link biz.thonbecker.personal.booking.api.BookingCreatedEvent} and {@link biz.thonbecker.personal.booking.api.BookingCancelledEvent}</li>
 *   <li>trivia - Imports quiz-related event types</li>
 *   <li>foosball - Imports game-related event types</li>
 *   <li>user - Imports user-related event types</li>
 * </ul>
 *
 * <p>Event Subscriptions:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.booking.api.BookingCreatedEvent} - Send booking confirmation email to attendee and admin</li>
 *   <li>{@link biz.thonbecker.personal.booking.api.BookingCancelledEvent} - Send cancellation notification to attendee</li>
 *   <li>QuizCompletedEvent, QuizStartedEvent, PlayerJoinedQuizEvent - Log quiz events</li>
 *   <li>GameRecordedEvent, PlayerCreatedEvent - Log foosball events</li>
 *   <li>UserRegisteredEvent, UserLoginEvent, UserProfileUpdatedEvent - Log user events</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification Services",
        allowedDependencies = {"shared", "booking", "trivia", "foosball", "user"})
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.notification;
