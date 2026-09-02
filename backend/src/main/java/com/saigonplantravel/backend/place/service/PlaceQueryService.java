package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceQueryService {

    private final PlaceRepository placeRepository;

    public PlaceQueryService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }

    public List<Place> findAllActiveBySlugsForScheduling(Collection<String> slugs) {
        return placeRepository.findAllActiveBySlugsForScheduling(slugs);
    }
}
