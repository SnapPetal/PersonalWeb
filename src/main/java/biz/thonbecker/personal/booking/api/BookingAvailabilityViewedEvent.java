package biz.thonbecker.personal.booking.api;

public record BookingAvailabilityViewedEvent(String distinctId, Long bookingTypeId, int availableSlotCount) {}
