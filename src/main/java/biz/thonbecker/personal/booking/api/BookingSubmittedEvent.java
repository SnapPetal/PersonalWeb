package biz.thonbecker.personal.booking.api;

public record BookingSubmittedEvent(String distinctId, Long bookingTypeId) {}
