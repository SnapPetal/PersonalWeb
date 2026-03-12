package biz.thonbecker.personal.booking.platform.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlotEntity, Long> {

    /**
     * Finds availability slots that overlap with the given time range.
     *
     * @param startTime Range start time
     * @param endTime Range end time
     * @return List of overlapping slots
     */
    @Query("""
        SELECT a FROM AvailabilitySlotEntity a
        WHERE a.startTime < :endTime
        AND a.endTime > :startTime
        ORDER BY a.startTime
        """)
    List<AvailabilitySlotEntity> findOverlappingSlots(
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Finds a specific availability slot by exact start and end time.
     *
     * @param startTime Slot start time
     * @param endTime Slot end time
     * @return Optional slot
     */
    Optional<AvailabilitySlotEntity> findByStartTimeAndEndTime(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Finds all slots ordered by start time.
     *
     * @return List of all slots
     */
    List<AvailabilitySlotEntity> findAllByOrderByStartTimeAsc();
}
