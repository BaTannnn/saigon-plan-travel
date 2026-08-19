package com.saigonplantravel.backend.media.cloudinary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media.cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {

    public boolean configured() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
