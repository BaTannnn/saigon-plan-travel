package com.saigonplantravel.backend.scheduling.exception;

public class ItineraryNotFoundException extends RuntimeException {

    public ItineraryNotFoundException() {
        super("Itinerary not found");
    }
}
