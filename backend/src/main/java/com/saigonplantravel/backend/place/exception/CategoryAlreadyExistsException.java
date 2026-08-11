package com.saigonplantravel.backend.place.exception;

import lombok.Getter;

@Getter
public class CategoryAlreadyExistsException extends RuntimeException {

    private final String fieldName;

    public CategoryAlreadyExistsException(String fieldName) {
        super(fieldName == null ? "Category name or slug already exists" : "Category " + fieldName + " already exists");
        this.fieldName = fieldName;
    }
}
