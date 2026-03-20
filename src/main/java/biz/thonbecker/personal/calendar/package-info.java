/**
 * Calendar Module
 *
 * <p>Manages external calendar integration via CalDAV (Nextcloud).
 * Listens to booking events to create/delete calendar entries and polls
 * for externally deleted events to trigger booking cancellations.
 *
 * <p>This module is fully event-driven:
 * <ul>
 *   <li>Listens to {@link biz.thonbecker.personal.booking.api.BookingCreatedEvent} to create calendar events</li>
 *   <li>Listens to {@link biz.thonbecker.personal.booking.api.BookingCancelledEvent} to delete calendar events</li>
 *   <li>Publishes {@link biz.thonbecker.personal.booking.api.BookingCancellationRequestedEvent}
 *       when a calendar event is deleted externally</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Calendar Integration",
        allowedDependencies = {"shared", "booking :: api"})
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.calendar;
