package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminCategoryResponse;
import com.saigonplantravel.backend.place.dto.admin.CategoryCreateRequest;
import com.saigonplantravel.backend.place.dto.admin.CategoryUpdateRequest;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.exception.CategoryAlreadyExistsException;
import com.saigonplantravel.backend.place.exception.CategoryNotFoundException;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.CategoryRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
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

    public List<AdminCategoryResponse> getCategoriesForAdministration() {
        return categoryRepository.findAllByOrderByNameAscIdAsc().stream()
                .map(placeMapper::toAdminCategoryResponse)
                .toList();
    }

    public AdminCategoryResponse getCategoryForAdministrationBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug).orElseThrow(CategoryNotFoundException::new);
        return placeMapper.toAdminCategoryResponse(category);
    }

    @Transactional
    public AdminCategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new CategoryAlreadyExistsException("name");
        }
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new CategoryAlreadyExistsException("slug");
        }

        try {
            Category category = categoryRepository.saveAndFlush(
                    new Category(request.name(), request.slug(), request.description()));
            return placeMapper.toAdminCategoryResponse(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(null);
        }
    }

    @Transactional
    public AdminCategoryResponse updateCategory(String slug, CategoryUpdateRequest request) {
        Category category = categoryRepository.findBySlug(slug).orElseThrow(CategoryNotFoundException::new);
        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new CategoryAlreadyExistsException("name");
        }

        category.updateDetails(request.name(), request.description());
        try {
            Category updatedCategory = categoryRepository.saveAndFlush(category);
            return placeMapper.toAdminCategoryResponse(updatedCategory);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException("name");
        }
    }
}
