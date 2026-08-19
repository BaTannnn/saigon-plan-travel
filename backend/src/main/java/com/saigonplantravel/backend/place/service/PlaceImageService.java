package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.media.MediaStorage;
import com.saigonplantravel.backend.media.MediaStorageException;
import com.saigonplantravel.backend.media.MediaUpload;
import com.saigonplantravel.backend.media.StoredMedia;
import com.saigonplantravel.backend.place.exception.InvalidPlaceImageException;
import com.saigonplantravel.backend.place.exception.PlaceImageException;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlaceImageService {

    static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Logger log = LoggerFactory.getLogger(PlaceImageService.class);

    private final MediaStorage mediaStorage;
    private final PlaceImageMetadataService metadataService;

    public PlaceImageService(MediaStorage mediaStorage, PlaceImageMetadataService metadataService) {
        this.mediaStorage = mediaStorage;
        this.metadataService = metadataService;
    }

    public void uploadCover(String slug, MultipartFile file) {
        validate(file);
        StoredMedia uploaded =
                mediaStorage.upload(new MediaUpload(read(file), file.getContentType(), storageKey(slug)));

        String oldStorageKey;
        try {
            oldStorageKey = metadataService.replaceCover(slug, uploaded);
        } catch (RuntimeException exception) {
            compensateUpload(uploaded.storageKey(), exception);
            if (exception instanceof PlaceImageException placeImageException) {
                throw placeImageException;
            }
            throw new PlaceImageException("Place image metadata could not be saved", exception);
        }

        if (oldStorageKey != null) {
            mediaStorage.delete(oldStorageKey);
        }
    }

    public void removeCover(String slug) {
        String storageKey = metadataService.removeCover(slug);
        if (storageKey != null) {
            mediaStorage.delete(storageKey);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPlaceImageException("Choose a non-empty image file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new InvalidPlaceImageException("The uploaded file must have an image MIME type");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidPlaceImageException("The Place image must not exceed 5 MB");
        }
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new InvalidPlaceImageException("The uploaded image could not be read");
        }
    }

    private String storageKey(String slug) {
        return "saigonplantravel/places/" + slug + "/cover-"
                + UUID.randomUUID().toString().replace("-", "");
    }

    private void compensateUpload(String storageKey, RuntimeException original) {
        try {
            mediaStorage.delete(storageKey);
        } catch (MediaStorageException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
            log.error("Failed to clean up uploaded Place image {} after metadata failure", storageKey, cleanupFailure);
        }
    }
}
