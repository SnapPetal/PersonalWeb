package biz.thonbecker.personal.booking.platform.service;

import biz.thonbecker.personal.booking.platform.persistence.AvailabilitySlotEntity;
import biz.thonbecker.personal.booking.platform.persistence.AvailabilitySlotRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatically generates recurring availability slots.
 *
 * <p>Creates availability slots for the configured weekly schedule:
 * <ul>
 *   <li>Monday-Friday: 11:00 AM - 12:00 PM, 6:00 PM - 9:00 PM
 *   <li>Generates slots for the next 4 weeks
 *   <li>Runs daily at 2:00 AM to maintain availability
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityScheduler {

    private final AvailabilitySlotRepository availabilitySlotRepository;

    // Weekly schedule configuration
    private static final LocalTime MORNING_START = LocalTime.of(11, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime EVENING_START = LocalTime.of(18, 0);
    private static final LocalTime EVENING_END = LocalTime.of(21, 0);
    private static final int WEEKS_AHEAD = 4;

    /**
     * Generates availability slots on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generateSlotsOnStartup() {
        log.info("Generating availability slots on application startup...");
        generateAvailabilitySlots();
    }

    /**
     * Scheduled job to maintain availability slots.
     *
     * <p>Runs daily at 2:00 AM to ensure there are always slots available for the next 4 weeks.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "generateAvailabilitySlots", lockAtLeastFor = "PT5M", lockAtMostFor = "PT10M")
    @Transactional
    public void scheduledGeneration() {
        log.info("Running scheduled availability slot generation...");
        generateAvailabilitySlots();
    }

    /**
     * Generates availability slots for the configured schedule.
     *
     * <p>Creates slots for Monday-Friday, 11 AM - 12 PM and 6 PM - 9 PM, for the next 4 weeks.
     * Only creates slots that don't already exist.
     */
    void generateAvailabilitySlots() {
        final var today = LocalDate.now();
        final var endDate = today.plusWeeks(WEEKS_AHEAD);
        final var slotsToCreate = new ArrayList<AvailabilitySlotEntity>();

        var currentDate = today;
        while (!currentDate.isAfter(endDate)) {
            // Only create slots for Monday-Friday
            final var dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                // Morning slot: 11 AM - 12 PM
                final var morningStart = LocalDateTime.of(currentDate, MORNING_START);
                final var morningEnd = LocalDateTime.of(currentDate, MORNING_END);
                if (shouldCreateSlot(morningStart, morningEnd)) {
                    slotsToCreate.add(createSlotEntity(morningStart, morningEnd));
                }

                // Evening slot: 6 PM - 9 PM
                final var eveningStart = LocalDateTime.of(currentDate, EVENING_START);
                final var eveningEnd = LocalDateTime.of(currentDate, EVENING_END);
                if (shouldCreateSlot(eveningStart, eveningEnd)) {
                    slotsToCreate.add(createSlotEntity(eveningStart, eveningEnd));
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        if (!slotsToCreate.isEmpty()) {
            availabilitySlotRepository.saveAll(slotsToCreate);
            log.info("Created {} new availability slots", slotsToCreate.size());
        } else {
            log.info("All availability slots already exist, no new slots created");
        }
    }

    /**
     * Checks if a slot should be created (doesn't already exist).
     *
     * @param startTime Slot start time
     * @param endTime Slot end time
     * @return true if the slot should be created
     */
    private boolean shouldCreateSlot(final LocalDateTime startTime, final LocalDateTime endTime) {
        // Check if slot already exists
        return availabilitySlotRepository
                .findByStartTimeAndEndTime(startTime, endTime)
                .isEmpty();
    }

    /**
     * Creates an availability slot entity.
     *
     * @param startTime Slot start time
     * @param endTime Slot end time
     * @return The created entity
     */
    private AvailabilitySlotEntity createSlotEntity(final LocalDateTime startTime, final LocalDateTime endTime) {
        final var slot = new AvailabilitySlotEntity();
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setMaxBookings(1);
        return slot;
    }

    /**
     * Deletes availability slots that are in the past.
     *
     * <p>Keeps the database clean by removing old slots.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "cleanupOldSlots", lockAtLeastFor = "PT5M", lockAtMostFor = "PT10M")
    @Transactional
    public void cleanupOldSlots() {
        final var now = LocalDateTime.now();
        final var allSlots = availabilitySlotRepository.findAll();

        final var oldSlots = allSlots.stream()
                .filter(slot -> slot.getEndTime().isBefore(now))
                .toList();

        if (!oldSlots.isEmpty()) {
            availabilitySlotRepository.deleteAll(oldSlots);
            log.info("Deleted {} old availability slots", oldSlots.size());
        }
    }
}
