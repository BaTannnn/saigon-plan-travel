package com.saigonplantravel.backend.place.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record OpeningHourResponse(
        short dayOfWeek,
        boolean closed,
        @JsonFormat(pattern = "HH:mm") LocalTime openTime,
        @JsonFormat(pattern = "HH:mm") LocalTime closeTime
) {
}
