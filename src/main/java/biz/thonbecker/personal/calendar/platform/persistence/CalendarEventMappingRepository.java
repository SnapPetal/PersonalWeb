package biz.thonbecker.personal.calendar.platform.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventMappingRepository extends JpaRepository<CalendarEventMappingEntity, Long> {

    Optional<CalendarEventMappingEntity> findByBookingId(Long bookingId);
}
