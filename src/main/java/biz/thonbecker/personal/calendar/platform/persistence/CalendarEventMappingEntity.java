package biz.thonbecker.personal.calendar.platform.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "calendar_event_mappings", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
public class CalendarEventMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "calendar_uid", nullable = false)
    private String calendarUid;

    @Column(name = "calendar_href")
    private String calendarHref;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
