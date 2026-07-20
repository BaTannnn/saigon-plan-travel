package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PlaceMapper placeMapper;

    public CategoryService(CategoryRepository categoryRepository, PlaceMapper placeMapper) {
        this.categoryRepository = categoryRepository;
        this.placeMapper = placeMapper;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByNameAscIdAsc().stream()
                .map(placeMapper::toCategoryResponse)
                .toList();
    }
}
