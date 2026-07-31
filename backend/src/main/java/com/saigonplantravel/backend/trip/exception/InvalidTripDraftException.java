package com.saigonplantravel.backend.trip.exception;

public class InvalidTripDraftException extends RuntimeException {

    private final String code;
    private final String field;

    public InvalidTripDraftException(
            String code,
            String field,
            String message
    ) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}