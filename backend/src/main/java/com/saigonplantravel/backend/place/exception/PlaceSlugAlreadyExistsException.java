package com.saigonplantravel.backend.place.exception;

public class PlaceSlugAlreadyExistsException extends RuntimeException {

    public PlaceSlugAlreadyExistsException() {
        super("Slug already exists");
    }
}
