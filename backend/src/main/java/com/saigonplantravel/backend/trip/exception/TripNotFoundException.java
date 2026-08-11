package com.saigonplantravel.backend.trip.exception;

public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException() {
        super("Trip not found");
    }
}
