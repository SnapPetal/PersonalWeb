package biz.thonbecker.personal.user.api;

public record LoginRequestedEvent(String distinctId, String redirectPath) {}
