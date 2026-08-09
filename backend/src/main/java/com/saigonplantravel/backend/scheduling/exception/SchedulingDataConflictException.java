package com.saigonplantravel.backend.scheduling.exception;

public class SchedulingDataConflictException extends RuntimeException {

    public SchedulingDataConflictException(String message) {
        super(message);
    }

    public SchedulingDataConflictException(
            String message,
            RuntimeException cause
    ) {
        super(message, cause);
    }
}
