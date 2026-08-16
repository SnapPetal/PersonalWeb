package biz.thonbecker.personal.user.api;

public record LoginCompletedEvent(String distinctId, String method) {}
