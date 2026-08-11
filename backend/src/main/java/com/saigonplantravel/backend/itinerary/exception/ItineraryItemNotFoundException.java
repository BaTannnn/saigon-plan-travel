package com.saigonplantravel.backend.itinerary.exception;

public class ItineraryItemNotFoundException extends RuntimeException {

    public ItineraryItemNotFoundException() {
        super("Itinerary item not found");
    }
}
