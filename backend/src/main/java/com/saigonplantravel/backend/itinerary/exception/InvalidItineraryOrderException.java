package com.saigonplantravel.backend.itinerary.exception;

public class InvalidItineraryOrderException extends RuntimeException {

    public InvalidItineraryOrderException() {
        super("Item order must contain every itinerary item exactly once");
    }
}
