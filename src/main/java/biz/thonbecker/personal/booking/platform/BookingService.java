package biz.thonbecker.personal.booking.platform;

import biz.thonbecker.personal.booking.api.*;
import biz.thonbecker.personal.booking.domain.exceptions.BookingNotFoundException;
import biz.thonbecker.personal.booking.domain.exceptions.BookingTypeNotFoundException;
import biz.thonbecker.personal.booking.domain.exceptions.InvalidBookingException;
import biz.thonbecker.personal.booking.domain.exceptions.SlotNotAvailableException;
import biz.thonbecker.personal.booking.platform.persistence.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking service providing appointment scheduling, availability management, and booking lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingTypeRepository bookingTypeRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CONFIRMATION_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String OVERLAP_CONSTRAINT = "booking_no_overlapping_confirmed";

    @Transactional(readOnly = true)
    public List<BookingType> getActiveBookingTypes() {
        log.debug("Fetching active booking types");
        return bookingTypeRepository.findByActiveTrue().stream()
                .map(this::convertBookingTypeToDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingType> getAllBookingTypes() {
        log.debug("Fetching all booking types");
        return bookingTypeRepository.findAll().stream()
                .map(this::convertBookingTypeToDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> getAvailableSlots(final Long bookingTypeId, final LocalDate date) {
        log.debug("Fetching available slots for booking type {} on {}", bookingTypeId, date);

        final var bookingType = bookingTypeRepository
                .findById(bookingTypeId)
                .orElseThrow(() -> new BookingTypeNotFoundException(bookingTypeId));

        final var dayStart = date.atStartOfDay();
        final var dayEnd = date.plusDays(1).atStartOfDay();

        // Find all availability slots for this day
        final var availabilitySlots = availabilitySlotRepository.findOverlappingSlots(dayStart, dayEnd);

        // Find all existing bookings for this day
        final var existingBookings = bookingRepository.findBookingsInRange(dayStart, dayEnd);

        // Generate time slots from availability
        final var slots = new ArrayList<TimeSlot>();
        for (final var availSlot : availabilitySlots) {
            final var slotStart = availSlot.getStartTime().isAfter(dayStart) ? availSlot.getStartTime() : dayStart;
            final var slotEnd = availSlot.getEndTime().isBefore(dayEnd) ? availSlot.getEndTime() : dayEnd;

            // Generate slots based on booking type duration
            var currentTime = slotStart;
            while (currentTime.plusMinutes(bookingType.getDurationMinutes()).isBefore(slotEnd)
                    || currentTime.plusMinutes(bookingType.getDurationMinutes()).equals(slotEnd)) {

                final var slotStartTime = currentTime;
                final var slotEndTime = currentTime.plusMinutes(bookingType.getDurationMinutes());

                // Check if this slot conflicts with any existing booking
                final var hasConflict = existingBookings.stream()
                        .anyMatch(booking -> booking.getStartTime().isBefore(slotEndTime)
                                && booking.getEndTime().isAfter(slotStartTime));

                if (!hasConflict && slotStartTime.isAfter(LocalDateTime.now())) {
                    slots.add(new TimeSlot(null, slotStartTime, slotEndTime, true));
                }

                // Move to next slot (duration + buffer)
                currentTime =
                        currentTime.plusMinutes(bookingType.getDurationMinutes() + bookingType.getBufferMinutes());
            }
        }

        log.info("Found {} available slots for booking type {} on {}", slots.size(), bookingTypeId, date);
        return slots;
    }

    @Transactional
    public Booking createBooking(
            final Long bookingTypeId,
            final String attendeeName,
            final String attendeeEmail,
            final String attendeePhone,
            final LocalDateTime startTime,
            final String message,
            final String userId) {

        log.info("Creating booking for type {} at {}", bookingTypeId, startTime);

        // Validate inputs
        if (attendeeName == null || attendeeName.isBlank()) {
            throw new InvalidBookingException("Attendee name is required");
        }
        if (attendeeEmail == null || attendeeEmail.isBlank()) {
            throw new InvalidBookingException("Attendee email is required");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot book time slots in the past");
        }

        // Get booking type
        final var bookingType = bookingTypeRepository
                .findById(bookingTypeId)
                .orElseThrow(() -> new BookingTypeNotFoundException(bookingTypeId));

        if (!bookingType.getActive()) {
            throw new InvalidBookingException("This booking type is no longer available");
        }

        final var endTime = startTime.plusMinutes(bookingType.getDurationMinutes());

        // Check if slot is available
        final var conflictingBookings = bookingRepository.findBookingsInRange(startTime, endTime);
        if (!conflictingBookings.isEmpty()) {
            throw new SlotNotAvailableException(startTime);
        }

        // Check if time falls within an availability slot
        final var availabilitySlots = availabilitySlotRepository.findOverlappingSlotsForUpdate(startTime, endTime);
        if (availabilitySlots.isEmpty()) {
            throw new SlotNotAvailableException("No availability slot exists for the selected time");
        }

        // Create booking
        final var booking = new BookingEntity();
        booking.setConfirmationCode(generateConfirmationCode());
        booking.setBookingType(bookingType);
        booking.setAttendeeName(attendeeName);
        booking.setAttendeeEmail(attendeeEmail);
        booking.setAttendeePhone(attendeePhone);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setMessage(message);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUserId(userId);

        final BookingEntity savedBooking;
        try {
            savedBooking = bookingRepository.saveAndFlush(booking);
        } catch (final DataIntegrityViolationException e) {
            if (isOverlapConstraintViolation(e)) {
                throw new SlotNotAvailableException(startTime);
            }
            throw e;
        }
        log.info("Created booking with confirmation code: {}", savedBooking.getConfirmationCode());

        // Publish event (notifications will be sent by event listener)
        final var event = new BookingCreatedEvent(
                savedBooking.getId(),
                savedBooking.getConfirmationCode(),
                savedBooking.getAttendeeEmail(),
                savedBooking.getAttendeeName(),
                savedBooking.getAttendeePhone(),
                savedBooking.getBookingType().getName(),
                savedBooking.getStartTime(),
                savedBooking.getEndTime(),
                savedBooking.getMessage());
        eventPublisher.publishEvent(event);

        return convertBookingToDomain(savedBooking);
    }

    @Transactional(readOnly = true)
    public Booking getBookingByConfirmationCode(final String confirmationCode) {
        log.debug("Fetching booking by confirmation code: {}", confirmationCode);
        return bookingRepository
                .findByConfirmationCode(confirmationCode)
                .map(this::convertBookingToDomain)
                .orElseThrow(() -> new BookingNotFoundException(confirmationCode));
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        log.debug("Fetching all bookings");
        return bookingRepository.findAllByOrderByStartTimeDesc().stream()
                .map(this::convertBookingToDomain)
                .toList();
    }

    @Transactional
    public void cancelBooking(final Long bookingId) {
        log.info("Cancelling booking {}", bookingId);

        final var booking =
                bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking {} is already cancelled", bookingId);
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Publish event (notifications will be sent by event listener)
        final var event = new BookingCancelledEvent(
                booking.getId(),
                booking.getConfirmationCode(),
                booking.getAttendeeEmail(),
                booking.getAttendeeName(),
                booking.getBookingType().getName(),
                booking.getStartTime(),
                booking.getEndTime());
        eventPublisher.publishEvent(event);

        log.info("Successfully cancelled booking {}", bookingId);
    }

    @Transactional
    public BookingType createBookingType(
            final String name,
            final String description,
            final int durationMinutes,
            final int bufferMinutes,
            final String color) {

        log.info("Creating booking type: {}", name);

        final var entity = new BookingTypeEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setDurationMinutes(durationMinutes);
        entity.setBufferMinutes(bufferMinutes);
        entity.setColor(color);
        entity.setActive(true);

        final var saved = bookingTypeRepository.save(entity);
        log.info("Created booking type with ID: {}", saved.getId());

        return convertBookingTypeToDomain(saved);
    }

    @Transactional
    public void createAvailabilitySlot(final LocalDateTime startTime, final LocalDateTime endTime) {
        log.info("Creating availability slot: {} - {}", startTime, endTime);

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new InvalidBookingException("End time must be after start time");
        }

        final var entity = new AvailabilitySlotEntity();
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setMaxBookings(1);

        availabilitySlotRepository.save(entity);
        log.info("Created availability slot");
    }

    @Transactional
    public void deleteAvailabilitySlot(final Long slotId) {
        log.info("Deleting availability slot {}", slotId);
        availabilitySlotRepository.deleteById(slotId);
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> getAllAvailabilitySlots() {
        log.debug("Fetching all availability slots");
        return availabilitySlotRepository.findAllByOrderByStartTimeAsc().stream()
                .map(e -> new TimeSlot(e.getId(), e.getStartTime(), e.getEndTime(), true))
                .toList();
    }

    private BookingType convertBookingTypeToDomain(final BookingTypeEntity entity) {
        return new BookingType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getBufferMinutes(),
                entity.getActive(),
                entity.getColor());
    }

    private Booking convertBookingToDomain(final BookingEntity entity) {
        return new Booking(
                entity.getId(),
                entity.getConfirmationCode(),
                convertBookingTypeToDomain(entity.getBookingType()),
                entity.getAttendeeName(),
                entity.getAttendeeEmail(),
                entity.getAttendeePhone(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getUserId(),
                entity.getCreatedAt());
    }

    private String generateConfirmationCode() {
        final var code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(CONFIRMATION_CODE_CHARS.charAt(RANDOM.nextInt(CONFIRMATION_CODE_CHARS.length())));
        }
        return code.toString();
    }

    private boolean isOverlapConstraintViolation(final DataIntegrityViolationException exception) {
        final var message = exception.getMostSpecificCause().getMessage();
        return message != null && message.contains(OVERLAP_CONSTRAINT);
    }
}
