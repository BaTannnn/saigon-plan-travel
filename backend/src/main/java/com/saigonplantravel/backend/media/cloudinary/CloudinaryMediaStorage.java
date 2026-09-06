package com.saigonplantravel.backend.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.saigonplantravel.backend.media.MediaStorage;
import com.saigonplantravel.backend.media.MediaStorageException;
import com.saigonplantravel.backend.media.MediaUpload;
import com.saigonplantravel.backend.media.StoredMedia;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryMediaStorage implements MediaStorage {

    private final CloudinaryProperties properties;
    private final Cloudinary cloudinary;
    private static final Logger log = LoggerFactory.getLogger(CloudinaryMediaStorage.class);

    public CloudinaryMediaStorage(CloudinaryProperties properties) {
        this.properties = properties;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true));
    }

    @Override
    public StoredMedia upload(MediaUpload upload) {
        requireConfiguration();
        try {
            Map<?, ?> result = cloudinary
                    .uploader()
                    .upload(
                            upload.content(),
                            ObjectUtils.asMap(
                                    "public_id",
                                    upload.storageKey(),
                                    "resource_type",
                                    "image",
                                    "overwrite",
                                    false,
                                    "unique_filename",
                                    false));
            return new StoredMedia(requiredResult(result, "public_id"), requiredResult(result, "secure_url"));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary Place image upload failed for storage key {}", upload.storageKey(), exception);
            throw new MediaStorageException("Place image upload failed", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        requireConfiguration();
        try {
            Map<?, ?> result = cloudinary
                    .uploader()
                    .destroy(storageKey, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
            String status = requiredResult(result, "result");
            if (!status.equals("ok") && !status.equals("not found")) {
                throw new MediaStorageException("Cloudinary did not confirm Place image deletion");
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof MediaStorageException mediaStorageException) {
                throw mediaStorageException;
            }
            throw new MediaStorageException("Place image deletion failed", exception);
        }
    }

    private String requiredResult(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new MediaStorageException("Cloudinary response did not contain " + key);
        }
        return value.toString();
    }

    private void requireConfiguration() {
        if (!properties.configured()) {
            throw new MediaStorageException("Cloudinary credentials are not configured");
        }
    }
}
