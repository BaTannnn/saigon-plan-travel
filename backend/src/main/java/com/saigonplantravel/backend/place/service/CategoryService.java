package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.CategoryRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<CategoryResponse> findCategoriesBySlugs(Collection<String> slugs) {
        if (slugs.isEmpty()) {
            return List.of();
        }

        return categoryRepository.findAllBySlugInOrderByNameAscIdAsc(slugs).stream()
                .map(placeMapper::toCategoryResponse)
                .toList();
    }

    public List<CategoryResponse> findCategoriesByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return categoryRepository.findAllByIdInOrderByNameAscIdAsc(ids).stream()
                .map(placeMapper::toCategoryResponse)
                .toList();
    }
}
