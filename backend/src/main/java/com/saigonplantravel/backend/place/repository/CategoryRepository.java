package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAscIdAsc();

    List<Category> findAllBySlugInOrderByNameAscIdAsc(
            Collection<String> slugs
    );

    List<Category> findAllByIdInOrderByNameAscIdAsc(
            Collection<Long> ids
    );
}