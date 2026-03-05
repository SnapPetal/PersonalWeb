package biz.thonbecker.personal.landscape.domain.exceptions;

/**
 * Thrown when the USDA Plants API call fails.
 */
public class PlantApiException extends RuntimeException {

    public PlantApiException(final String message) {
        super(message);
    }

    public PlantApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
