package biz.thonbecker.personal.calendar.platform;

import biz.thonbecker.personal.booking.api.BookingCreatedEvent;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Service for interacting with Nextcloud CalDAV API.
 *
 * <p>Manages a "Bookings" calendar on Nextcloud, creating and deleting events
 * when bookings are created or cancelled. Supports polling for externally deleted events.
 */
@Service
@ConditionalOnProperty(name = "calendar.nextcloud.enabled", havingValue = "true")
@Slf4j
class NextcloudCalDavService {

    private final WebClient webClient;
    private final IcsGenerator icsGenerator;
    private final CalendarProperties properties;
    private final String calendarPath;

    NextcloudCalDavService(
            final IcsGenerator icsGenerator,
            final CalendarProperties properties,
            final WebClient.Builder webClientBuilder) {
        this.icsGenerator = icsGenerator;
        this.properties = properties;

        this.calendarPath =
                "/remote.php/dav/calendars/" + properties.username() + "/" + properties.calendarName() + "/";

        final var authHeader = "Basic "
                + Base64.getEncoder()
                        .encodeToString(
                                (properties.username() + ":" + properties.password()).getBytes(StandardCharsets.UTF_8));

        this.webClient = webClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", authHeader)
                .build();
    }

    /**
     * Ensures the "Bookings" calendar exists in Nextcloud.
     * Creates it via MKCALENDAR if it doesn't exist.
     */
    @Retryable(maxAttempts = 3)
    public void ensureCalendarExists() {
        try {
            final var response = webClient
                    .method(HttpMethod.valueOf("PROPFIND"))
                    .uri(calendarPath)
                    .header("Depth", "0")
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            if (Objects.nonNull(response) && response.getStatusCode().is2xxSuccessful()) {
                log.info("Nextcloud Bookings calendar already exists");
                return;
            }
        } catch (final Exception e) {
            log.debug("Calendar does not exist yet, creating it: {}", e.getMessage());
        }

        final var mkCalendarBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <c:mkcalendar xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:set>
                    <d:prop>
                      <d:displayname>Bookings</d:displayname>
                      <c:supported-calendar-component-set>
                        <c:comp name="VEVENT"/>
                      </c:supported-calendar-component-set>
                    </d:prop>
                  </d:set>
                </c:mkcalendar>
                """;

        try {
            webClient
                    .method(HttpMethod.valueOf("MKCALENDAR"))
                    .uri(calendarPath)
                    .contentType(MediaType.APPLICATION_XML)
                    .bodyValue(mkCalendarBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Created Nextcloud Bookings calendar at {}", calendarPath);
        } catch (final Exception e) {
            log.error("Failed to create Nextcloud Bookings calendar: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Nextcloud calendar", e);
        }
    }

    @Retryable(maxAttempts = 3)
    public String createEvent(final BookingCreatedEvent event) {
        final var calendarUid = "booking-" + event.confirmationCode();
        final var icsContent = icsGenerator.generate(event, properties.organizerEmail(), calendarUid);
        final var eventHref = calendarPath + calendarUid + ".ics";

        webClient
                .put()
                .uri(eventHref)
                .header("Content-Type", "text/calendar; charset=utf-8")
                .header("If-None-Match", "*")
                .bodyValue(icsContent.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("Created CalDAV event {} for booking {}", calendarUid, event.confirmationCode());
        return calendarUid;
    }

    @Retryable(maxAttempts = 3)
    public void deleteEvent(final String calendarUid) {
        final var eventHref = calendarPath + calendarUid + ".ics";

        try {
            webClient.delete().uri(eventHref).retrieve().toBodilessEntity().block();
            log.info("Deleted CalDAV event {}", calendarUid);
        } catch (final Exception e) {
            log.warn("Failed to delete CalDAV event {} (may already be deleted): {}", calendarUid, e.getMessage());
        }
    }

    @Retryable(maxAttempts = 3)
    public List<String> listEventResources() {
        final var propfindBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop>
                    <d:getetag/>
                  </d:prop>
                </d:propfind>
                """;

        final var responseBody = webClient
                .method(HttpMethod.valueOf("PROPFIND"))
                .uri(calendarPath)
                .header("Depth", "1")
                .contentType(MediaType.APPLICATION_XML)
                .bodyValue(propfindBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseHrefsFromMultistatus(responseBody);
    }

    private List<String> parseHrefsFromMultistatus(final String xml) {
        final var uids = new ArrayList<String>();
        if (Objects.isNull(xml) || xml.isBlank()) {
            return uids;
        }

        try {
            final var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            final var builder = factory.newDocumentBuilder();
            final var document = builder.parse(new InputSource(new StringReader(xml)));

            final var hrefNodes = document.getElementsByTagNameNS("DAV:", "href");
            for (int i = 0; i < hrefNodes.getLength(); i++) {
                final var href = ((Element) hrefNodes.item(i)).getTextContent().trim();
                if (href.endsWith(".ics")) {
                    final var fileName = href.substring(href.lastIndexOf('/') + 1);
                    final var uid = fileName.replace(".ics", "");
                    uids.add(uid);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to parse CalDAV PROPFIND response: {}", e.getMessage(), e);
        }

        return uids;
    }
}
