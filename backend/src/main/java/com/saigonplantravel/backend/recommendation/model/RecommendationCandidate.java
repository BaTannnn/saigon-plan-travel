package com.saigonplantravel.backend.recommendation.model;

import com.saigonplantravel.backend.place.entity.Place;

public record RecommendationCandidate(Place place, double semanticScore, String matchedSection) {}
