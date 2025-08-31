package solutions.thonbecker.personal.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoosballPlayer {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("created_at")
    private String createdAt;
}

// If your API returns wrapped responses like {"data": [...], "status": "success"},
// you might need a wrapper class like this:
/*
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoosballPlayerResponse {
    @JsonProperty("data")
    private List<FoosballPlayer> data;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
}
*/
