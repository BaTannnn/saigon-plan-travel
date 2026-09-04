package com.saigonplantravel.backend.scheduling.model;

import java.time.LocalTime;

public record OpeningWindow(Status status, LocalTime openTime, LocalTime closeTime) {

    public static OpeningWindow unknown() {
        return new OpeningWindow(Status.UNKNOWN, null, null);
    }

    public static OpeningWindow closed() {
        return new OpeningWindow(Status.CLOSED, null, null);
    }

    public static OpeningWindow open(LocalTime openTime, LocalTime closeTime) {
        return new OpeningWindow(Status.OPEN, openTime, closeTime);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public enum Status {
        UNKNOWN,
        CLOSED,
        OPEN
    }
}
