package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.media.StoredMedia;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.entity.PlaceImage;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.repository.PlaceImageRepository;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceImageMetadataService {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;

    PlaceImageMetadataService(PlaceRepository placeRepository, PlaceImageRepository placeImageRepository) {
        this.placeRepository = placeRepository;
        this.placeImageRepository = placeImageRepository;
    }

    @Transactional
    public String replaceCover(String slug, StoredMedia media) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);
        String oldStorageKey = place.getPrimaryImageStorageKey();

        if (oldStorageKey != null) {
            place.removeCoverImage();
            placeRepository.flush();
        }

        PlaceImage image = new PlaceImage(place, media.storageKey(), media.url(), place.getName());
        place.replaceCoverImage(image);
        placeImageRepository.saveAndFlush(image);
        return oldStorageKey;
    }

    @Transactional
    public String removeCover(String slug) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);
        String storageKey = place.getPrimaryImageStorageKey();
        if (storageKey == null) {
            return null;
        }

        place.removeCoverImage();
        placeRepository.flush();
        return storageKey;
    }
}
