package com.saigonplantravel.backend.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.place.dto.admin.AdminCategoryResponse;
import com.saigonplantravel.backend.place.dto.admin.CategoryCreateRequest;
import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.admin.CategoryUpdateRequest;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.exception.CategoryAlreadyExistsException;
import com.saigonplantravel.backend.place.exception.CategoryNotFoundException;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
        when(categoryRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(science, culture));
        when(placeMapper.toCategoryResponse(science)).thenReturn(scienceResponse);
        when(placeMapper.toCategoryResponse(culture)).thenReturn(cultureResponse);

        List<CategoryResponse> response = new CategoryService(categoryRepository, placeMapper).getCategories();

        assertThat(response).containsExactly(scienceResponse, cultureResponse);
        verify(categoryRepository).findAllByOrderByNameAscIdAsc();
    }

    @Test
    void returnsEmptyArrayWhenCatalogIsEmpty() {
        when(categoryRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of());

        List<CategoryResponse> response = new CategoryService(categoryRepository, placeMapper).getCategories();

        assertThat(response).isEmpty();
    }

    @Test
    void findsCategoriesBySlugsInOneBatchQuery() {
        Category culture = mock(Category.class);
        Category art = mock(Category.class);

        CategoryResponse artResponse = new CategoryResponse(2L, "Nghệ thuật", "nghe-thuat");

        CategoryResponse cultureResponse = new CategoryResponse(1L, "Văn hóa", "van-hoa");

        Set<String> requestedSlugs = Set.of("van-hoa", "nghe-thuat");

        when(categoryRepository.findAllBySlugInOrderByNameAscIdAsc(requestedSlugs))
                .thenReturn(List.of(art, culture));

        when(placeMapper.toCategoryResponse(art)).thenReturn(artResponse);

        when(placeMapper.toCategoryResponse(culture)).thenReturn(cultureResponse);

        CategoryService service = new CategoryService(categoryRepository, placeMapper);

        List<CategoryResponse> response = service.findCategoriesBySlugs(requestedSlugs);

        assertThat(response).containsExactly(artResponse, cultureResponse);

        verify(categoryRepository).findAllBySlugInOrderByNameAscIdAsc(requestedSlugs);
    }

    @Test
    void findsCategoriesByIdsInOneBatchQuery() {
        Category culture = mock(Category.class);
        Category art = mock(Category.class);

        CategoryResponse artResponse = new CategoryResponse(2L, "Nghệ thuật", "nghe-thuat");

        CategoryResponse cultureResponse = new CategoryResponse(1L, "Văn hóa", "van-hoa");

        Set<Long> requestedIds = Set.of(1L, 2L);

        when(categoryRepository.findAllByIdInOrderByNameAscIdAsc(requestedIds)).thenReturn(List.of(art, culture));

        when(placeMapper.toCategoryResponse(art)).thenReturn(artResponse);

        when(placeMapper.toCategoryResponse(culture)).thenReturn(cultureResponse);

        CategoryService service = new CategoryService(categoryRepository, placeMapper);

        List<CategoryResponse> response = service.findCategoriesByIds(requestedIds);

        assertThat(response).containsExactly(artResponse, cultureResponse);

        verify(categoryRepository).findAllByIdInOrderByNameAscIdAsc(requestedIds);
    }

    @Test
    void doesNotQueryRepositoryForEmptyBatchInput() {
        CategoryService service = new CategoryService(categoryRepository, placeMapper);

        List<CategoryResponse> bySlugs = service.findCategoriesBySlugs(Set.of());

        List<CategoryResponse> byIds = service.findCategoriesByIds(Set.of());

        assertThat(bySlugs).isEmpty();
        assertThat(byIds).isEmpty();

        verify(categoryRepository, never()).findAllBySlugInOrderByNameAscIdAsc(Set.of());

        verify(categoryRepository, never()).findAllByIdInOrderByNameAscIdAsc(Set.of());
    }

    @Test
    void returnsAdministrativeCatalogWithDescriptions() {
        Category category = mock(Category.class);
        AdminCategoryResponse response = new AdminCategoryResponse(1L, "Science", "science", "Science places");
        when(categoryRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(category));
        when(placeMapper.toAdminCategoryResponse(category)).thenReturn(response);

        assertThat(new CategoryService(categoryRepository, placeMapper).getCategoriesForAdministration())
                .containsExactly(response);
    }

    @Test
    void createsCategoryWithNormalizedValues() {
        CategoryCreateRequest request = new CategoryCreateRequest("  Science  ", "  science  ", "   ");
        AdminCategoryResponse response = new AdminCategoryResponse(6L, "Science", "science", null);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(placeMapper.toAdminCategoryResponse(any(Category.class))).thenReturn(response);

        AdminCategoryResponse created = new CategoryService(categoryRepository, placeMapper).createCategory(request);

        assertThat(created).isEqualTo(response);
        verify(categoryRepository).existsByName("Science");
        verify(categoryRepository).existsBySlug("science");
        verify(categoryRepository)
                .saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                        category -> category.getName().equals("Science")
                                && category.getSlug().equals("science")
                                && category.getDescription() == null));
    }

    @Test
    void rejectsDuplicateNameBeforeInsert() {
        CategoryCreateRequest request = new CategoryCreateRequest("Science", "science", null);
        when(categoryRepository.existsByName("Science")).thenReturn(true);

        assertThatThrownBy(() -> new CategoryService(categoryRepository, placeMapper).createCategory(request))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .extracting(exception -> ((CategoryAlreadyExistsException) exception).getFieldName())
                .isEqualTo("name");
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void mapsDatabaseUniquenessRaceToCategoryConflict() {
        CategoryCreateRequest request = new CategoryCreateRequest("Science", "science", null);
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> new CategoryService(categoryRepository, placeMapper).createCategory(request))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Category name or slug already exists");
    }

    @Test
    void returnsAdministrativeCategoryBySlug() {
        Category category = mock(Category.class);
        AdminCategoryResponse response = new AdminCategoryResponse(1L, "Science", "science", "Science places");
        when(categoryRepository.findBySlug("science")).thenReturn(Optional.of(category));
        when(placeMapper.toAdminCategoryResponse(category)).thenReturn(response);

        assertThat(new CategoryService(categoryRepository, placeMapper).getCategoryForAdministrationBySlug("science"))
                .isEqualTo(response);
    }

    @Test
    void updatesCategoryDetailsWithoutChangingSlug() {
        Category category = mock(Category.class);
        CategoryUpdateRequest request = new CategoryUpdateRequest("  Natural Science  ", "   ");
        AdminCategoryResponse response = new AdminCategoryResponse(1L, "Natural Science", "science", null);
        when(categoryRepository.findBySlug("science")).thenReturn(Optional.of(category));
        when(category.getName()).thenReturn("Science");
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);
        when(placeMapper.toAdminCategoryResponse(category)).thenReturn(response);

        AdminCategoryResponse updated =
                new CategoryService(categoryRepository, placeMapper).updateCategory("science", request);

        assertThat(updated).isEqualTo(response);
        verify(category).updateDetails("Natural Science", null);
        verify(categoryRepository).existsByName("Natural Science");
        verify(categoryRepository).saveAndFlush(category);
    }

    @Test
    void allowsKeepingCurrentCategoryName() {
        Category category = mock(Category.class);
        CategoryUpdateRequest request = new CategoryUpdateRequest("Science", "Updated");
        when(categoryRepository.findBySlug("science")).thenReturn(Optional.of(category));
        when(category.getName()).thenReturn("Science");
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        new CategoryService(categoryRepository, placeMapper).updateCategory("science", request);

        verify(categoryRepository, never()).existsByName("Science");
        verify(category).updateDetails("Science", "Updated");
    }

    @Test
    void rejectsDuplicateNameBeforeCategoryUpdate() {
        Category category = mock(Category.class);
        CategoryUpdateRequest request = new CategoryUpdateRequest("Culture", null);
        when(categoryRepository.findBySlug("science")).thenReturn(Optional.of(category));
        when(category.getName()).thenReturn("Science");
        when(categoryRepository.existsByName("Culture")).thenReturn(true);

        assertThatThrownBy(
                        () -> new CategoryService(categoryRepository, placeMapper).updateCategory("science", request))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .extracting(exception -> ((CategoryAlreadyExistsException) exception).getFieldName())
                .isEqualTo("name");
        verify(category, never()).updateDetails(any(), any());
    }

    @Test
    void rejectsMissingCategoryBeforeUpdate() {
        when(categoryRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CategoryService(categoryRepository, placeMapper)
                        .updateCategory("missing", new CategoryUpdateRequest("Science", null)))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
