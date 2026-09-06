package com.saigonplantravel.backend.itinerary.exception;

public class InvalidGeneratedItineraryException extends RuntimeException {

    public InvalidGeneratedItineraryException() {
        super("Generated itinerary must contain valid, active, unique places");
    }
}
