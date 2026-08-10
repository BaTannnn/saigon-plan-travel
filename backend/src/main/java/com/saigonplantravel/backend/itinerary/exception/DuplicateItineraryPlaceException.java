package com.saigonplantravel.backend.itinerary.exception;

public class DuplicateItineraryPlaceException
        extends RuntimeException {

    public DuplicateItineraryPlaceException() {
        super("Place already exists in this itinerary");
    }
}
