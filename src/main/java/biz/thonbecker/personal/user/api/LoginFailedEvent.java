package biz.thonbecker.personal.user.api;

public record LoginFailedEvent(String distinctId, String reason) {}
