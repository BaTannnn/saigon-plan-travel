package com.saigonplantravel.backend.place.exception;

public class PlaceNotFoundException extends RuntimeException {

    public PlaceNotFoundException() {
        super("Place not found");
    }
}
