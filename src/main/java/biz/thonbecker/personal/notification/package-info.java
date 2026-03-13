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
 *   <li>shared - For infrastructure services</li>
 *   <li>booking - For fetching booking details and sending booking-related emails</li>
 * </ul>
 *
 * <p>Event Subscriptions:
 * <ul>
 *   <li>BookingCreatedEvent - Send booking confirmation email to attendee and admin</li>
 *   <li>BookingCancelledEvent - Send cancellation notification to attendee</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification Services",
        allowedDependencies = {"shared", "booking"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.notification;
