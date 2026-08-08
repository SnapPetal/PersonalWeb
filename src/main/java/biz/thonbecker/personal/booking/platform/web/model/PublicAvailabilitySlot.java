package biz.thonbecker.personal.booking.platform.web.model;

import java.time.OffsetDateTime;

/** A bookable slot without attendee or booking data. */
public record PublicAvailabilitySlot(
        Long bookingTypeId, String bookingTypeName, int durationMinutes, OffsetDateTime start, OffsetDateTime end) {}
