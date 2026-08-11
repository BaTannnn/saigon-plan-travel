package com.saigonplantravel.backend.trip.dto;

import java.math.BigDecimal;

public record StartLocationResponse(String label, BigDecimal latitude, BigDecimal longitude) {}
