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
 * <p>Module Dependencies (event types only, no method calls):
 * <ul>
 *   <li>shared - For infrastructure configuration</li>
 *   <li>booking - For BookingCreatedEvent, BookingCancelledEvent</li>
 *   <li>trivia - For QuizStartedEvent, QuizCompletedEvent, PlayerJoinedQuizEvent</li>
 *   <li>foosball - For GameRecordedEvent, PlayerCreatedEvent</li>
 *   <li>user - For UserRegisteredEvent, UserLoginEvent, UserProfileUpdatedEvent</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification Services",
        allowedDependencies = {"shared", "booking :: api", "trivia :: api", "foosball :: api", "user :: api"})
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.notification;
