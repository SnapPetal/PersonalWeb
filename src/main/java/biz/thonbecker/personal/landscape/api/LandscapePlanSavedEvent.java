package biz.thonbecker.personal.landscape.api;

public record LandscapePlanSavedEvent(String distinctId, Long planId, String hardinessZone) {}
