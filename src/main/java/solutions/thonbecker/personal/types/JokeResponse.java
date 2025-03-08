package solutions.thonbecker.personal.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JokeResponse(
        String Expiration, String ETag, String ServerSideEncryption, String Location, String Bucket) {}
