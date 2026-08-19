package com.saigonplantravel.backend.media;

public interface MediaStorage {
    StoredMedia upload(MediaUpload upload);

    void delete(String storageKey);
}
