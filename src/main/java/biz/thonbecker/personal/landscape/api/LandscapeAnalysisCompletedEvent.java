package biz.thonbecker.personal.landscape.api;

public record LandscapeAnalysisCompletedEvent(String distinctId, Long planId, String hardinessZone) {}
