package com.saigonplantravel.backend.scheduling.model;

public record SchedulingCandidate(SchedulingPlace place, double semanticScore, String matchedSection) {}
