package com.saigonplantravel.backend.place.exception;

public class PlaceImageException extends RuntimeException {

    public PlaceImageException(String message) {
        super(message);
    }

    public PlaceImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
