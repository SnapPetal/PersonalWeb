package biz.thonbecker.personal.calendar.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import biz.thonbecker.personal.IntegrationTest;
import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import java.time.LocalDateTime;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@Import(CalendarEventPublicationRegistryTest.FailingCalDavConfig.class)
@TestPropertySource(properties = "calendar.nextcloud.enabled=true")
class CalendarEventPublicationRegistryTest {

    private final JdbcTemplate jdbcTemplate;

    CalendarEventPublicationRegistryTest(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void eventPublicationTableMatchesPinnedModulithShape() {
        final var columns = Set.copyOf(jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_name = 'event_publication'
                """, String.class));

        assertThat(columns)
                .contains(
                        "id",
                        "listener_id",
                        "event_type",
                        "serialized_event",
                        "publication_date",
                        "completion_date",
                        "completion_attempts",
                        "last_resubmission_date",
                        "status");
    }

    @Test
    void failingApplicationModuleListenerLeavesOutstandingPublication(final Scenario scenario) {
        final var before = outstandingPublicationCount();
        final var event = new BookingCreatedEvent(
                99L,
                "MODULTH1",
                "calendar@example.com",
                "Calendar Test",
                null,
                "Consultation",
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(30),
                null);

        scenario.publish(event).andWaitForStateChange(() -> outstandingPublicationCount() > before);

        assertThat(outstandingPublicationCount()).isGreaterThan(before);
    }

    private int outstandingPublicationCount() {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from event_publication
                where completion_date is null
                """, Integer.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingCalDavConfig {

        @Bean
        @Primary
        NextcloudCalDavService nextcloudCalDavService() {
            final var service = Mockito.mock(NextcloudCalDavService.class);
            doThrow(new IllegalStateException("Simulated CalDAV failure"))
                    .when(service)
                    .createEvent(any());
            return service;
        }
    }
}
