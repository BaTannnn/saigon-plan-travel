package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.entity.Category;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderByNameAscIdAsc();

    List<Category> findAllBySlugInOrderByNameAscIdAsc(Collection<String> slugs);

    List<Category> findAllByIdInOrderByNameAscIdAsc(Collection<Long> ids);
}
