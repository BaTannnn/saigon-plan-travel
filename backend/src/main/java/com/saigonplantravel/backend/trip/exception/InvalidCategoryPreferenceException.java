package com.saigonplantravel.backend.trip.exception;

import lombok.Getter;

import java.util.Collection;
import java.util.List;

@Getter
public class InvalidCategoryPreferenceException extends RuntimeException {

    private final List<String> unknownCategorySlugs;

    public InvalidCategoryPreferenceException(
            Collection<String> unknownCategorySlugs
    ) {
        super(
                "One or more category preferences do not exist"
        );

        this.unknownCategorySlugs =
                unknownCategorySlugs.stream()
                        .sorted()
                        .toList();
    }

}