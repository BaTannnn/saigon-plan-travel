package com.saigonplantravel.backend.itinerary.exception;

public class InactiveItineraryPlaceException extends RuntimeException {

    public InactiveItineraryPlaceException() {
        super("Inactive Place cannot be added to an itinerary");
    }
}
