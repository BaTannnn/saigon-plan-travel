package com.saigonplantravel.backend.trip.exception;

import lombok.Getter;

@Getter
public class InvalidTripException extends RuntimeException {

    private final String code;
    private final String field;

    public InvalidTripException(
            String code,
            String field,
            String message
    ) {
        super(message);
        this.code = code;
        this.field = field;
    }

}