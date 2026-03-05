package biz.thonbecker.personal.landscape.domain.exceptions;

/**
 * Thrown when a landscape plan cannot be found by its ID.
 */
public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(final Long planId) {
        super("Landscape plan not found: " + planId);
    }

    public PlanNotFoundException(final String message) {
        super(message);
    }
}
