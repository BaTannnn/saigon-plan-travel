package com.saigonplantravel.backend.place.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceSearchNormalizerTest {

    @Test
    void normalizesUnicodeWhitespaceAndBlankValues() {
        assertThat(PlaceSearchNormalizer.normalizeText("  Bảo\t  tàng  ")).isEqualTo("Bảo tàng");
        assertThat(PlaceSearchNormalizer.normalizeText("   \n\t ")).isNull();
        assertThat(PlaceSearchNormalizer.normalizeText(null)).isNull();
    }

    @Test
    void escapesEveryLikeControlCharacter() {
        assertThat(PlaceSearchNormalizer.escapeLikePattern("50%_path\\name")).isEqualTo("50\\%\\_path\\\\name");
    }
}
