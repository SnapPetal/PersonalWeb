package biz.thonbecker.personal.landscape.api;

public record LandscapePlantAddedEvent(String distinctId, Long planId, String plantSymbol) {}
