package com.saigonplantravel.backend.place.exception;

import java.util.Collection;
import java.util.List;
import lombok.Getter;

@Getter
public class InvalidPlaceCategoryAssignmentException extends RuntimeException {

    private final List<String> unknownCategorySlugs;

    public InvalidPlaceCategoryAssignmentException(Collection<String> unknownCategorySlugs) {
        super("One or more selected categories do not exist");
        this.unknownCategorySlugs = unknownCategorySlugs.stream().sorted().toList();
    }
}
