package solutions.thonbecker.personal.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JokeResponse(
        boolean success,
        String message,
        JokeData data,
        String text,
        String voice,
        String translation) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JokeData(
            String Expiration, 
            String ETag, 
            String ServerSideEncryption, 
            String Location, 
            String Bucket) {}
}
