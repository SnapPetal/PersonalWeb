package biz.thonbecker.personal.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadJokeApiResponse(String id, String joke, int status) {}
