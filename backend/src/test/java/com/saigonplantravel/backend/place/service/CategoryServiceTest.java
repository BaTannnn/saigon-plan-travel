package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PlaceMapper placeMapper;

    @Test
    void returnsRepositoryOrderingAndMapsEveryCategory() {
        Category science = mock(Category.class);
        Category culture = mock(Category.class);
        CategoryResponse scienceResponse = new CategoryResponse(1L, "Khoa học", "khoa-hoc");
        CategoryResponse cultureResponse = new CategoryResponse(2L, "Văn hóa", "van-hoa");
        when(categoryRepository.findAllByOrderByNameAscIdAsc())
                .thenReturn(List.of(science, culture));
        when(placeMapper.toCategoryResponse(science)).thenReturn(scienceResponse);
        when(placeMapper.toCategoryResponse(culture)).thenReturn(cultureResponse);

        List<CategoryResponse> response = new CategoryService(categoryRepository, placeMapper)
                .getCategories();

        assertThat(response).containsExactly(scienceResponse, cultureResponse);
        verify(categoryRepository).findAllByOrderByNameAscIdAsc();
    }

    @Test
    void returnsEmptyArrayWhenCatalogIsEmpty() {
        when(categoryRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of());

        List<CategoryResponse> response = new CategoryService(categoryRepository, placeMapper)
                .getCategories();

        assertThat(response).isEmpty();
    }
}
