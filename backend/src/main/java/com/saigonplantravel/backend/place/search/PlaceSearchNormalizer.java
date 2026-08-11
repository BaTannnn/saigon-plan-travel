package com.saigonplantravel.backend.place.search;

import java.text.Normalizer;

public final class PlaceSearchNormalizer {

    public static final char LIKE_ESCAPE_CHARACTER = '\\';

    private PlaceSearchNormalizer() {}

    public static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized =
                Normalizer.normalize(value, Normalizer.Form.NFC).trim().replaceAll("[\\p{Z}\\s]+", " ");

        return normalized.isEmpty() ? null : normalized;
    }

    public static String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
