package biz.thonbecker.personal.booking.platform.web.model;

import biz.thonbecker.personal.booking.api.Booking;
import biz.thonbecker.personal.booking.api.BookingType;
import biz.thonbecker.personal.booking.api.TimeSlot;
import java.util.List;

/** Read model used by the external booking administration client. */
public record BookingAdminSnapshot(
        List<BookingType> bookingTypes, List<Booking> bookings, List<TimeSlot> availabilitySlots) {}
