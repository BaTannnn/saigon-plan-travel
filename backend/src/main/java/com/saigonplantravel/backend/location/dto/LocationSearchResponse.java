package com.saigonplantravel.backend.location.dto;

import java.math.BigDecimal;

public record LocationSearchResponse(String label, BigDecimal latitude, BigDecimal longitude) {}
