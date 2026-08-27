package com.saigonplantravel.backend.recommendation.model;

public record SemanticPlaceCandidate(String placeSlug, double semanticScore, String matchedSection) {}
