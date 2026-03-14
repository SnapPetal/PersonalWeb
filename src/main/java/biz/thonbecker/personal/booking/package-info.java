/**
 * Booking module for appointment scheduling.
 *
 * <p>This module provides appointment booking functionality with calendar integration and email
 * notifications.
 *
 * <h2>Public API</h2>
 *
 * External modules interact with this module through {@link
 * biz.thonbecker.personal.booking.platform.BookingService}.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Multiple booking types with configurable durations
 *   <li>Flexible availability management
 *   <li>Email notifications with iCal attachments
 *   <li>Booking confirmation and cancellation
 *   <li>Admin management interface
 * </ul>
 *
 * <h2>Domain Events</h2>
 *
 * <ul>
 *   <li>{@link biz.thonbecker.personal.booking.api.BookingCreatedEvent} - Published when a new
 *       booking is created
 *   <li>{@link biz.thonbecker.personal.booking.api.BookingCancelledEvent} - Published when a
 *       booking is cancelled
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Booking")
package biz.thonbecker.personal.booking;
