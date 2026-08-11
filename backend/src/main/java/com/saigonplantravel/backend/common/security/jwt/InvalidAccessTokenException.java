package com.saigonplantravel.backend.common.security.jwt;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("invalid or expired access token");
    }

    public InvalidAccessTokenException(Throwable cause) {
        super("invalid or expired access token", cause);
    }
}
