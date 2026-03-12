package biz.thonbecker.personal.booking.platform.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    /**
     * Finds a booking by confirmation code.
     *
     * @param confirmationCode Confirmation code
     * @return Optional booking
     */
    Optional<BookingEntity> findByConfirmationCode(String confirmationCode);

    /**
     * Finds all bookings for a specific user.
     *
     * @param userId User identifier
     * @return List of user's bookings
     */
    List<BookingEntity> findByUserIdOrderByStartTimeDesc(String userId);

    /**
     * Finds all bookings ordered by start time descending.
     *
     * @return List of all bookings
     */
    List<BookingEntity> findAllByOrderByStartTimeDesc();

    /**
     * Counts confirmed bookings in a specific time range.
     *
     * @param startTime Range start time
     * @param endTime Range end time
     * @return Count of bookings
     */
    @Query("""
        SELECT COUNT(b) FROM BookingEntity b
        WHERE b.status = 'CONFIRMED'
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        """)
    long countConfirmedBookingsInRange(
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Finds confirmed bookings in a specific time range.
     *
     * @param startTime Range start time
     * @param endTime Range end time
     * @return List of bookings
     */
    @Query("""
        SELECT b FROM BookingEntity b
        WHERE b.status IN ('CONFIRMED', 'PENDING')
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        ORDER BY b.startTime
        """)
    List<BookingEntity> findBookingsInRange(
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
