package biz.thonbecker.personal.content.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DadJokeApiResponse(String id, String joke, int status) {}
