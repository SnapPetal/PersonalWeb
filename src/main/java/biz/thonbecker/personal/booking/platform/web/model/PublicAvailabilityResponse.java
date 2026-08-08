package biz.thonbecker.personal.booking.platform.web.model;

import java.util.List;

/** Read-only availability returned to the public chat Worker. */
public record PublicAvailabilityResponse(String timezone, List<PublicAvailabilitySlot> slots) {}
