package com.saigonplantravel.backend.place.repository.specification;

import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.search.PlaceSearchNormalizer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

public final class PlaceSpecifications {

    private PlaceSpecifications() {}

    public static Specification<Place> matching(PlaceSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            if (request.keyword() != null) {
                predicates.add(keywordPredicate(root, criteriaBuilder, request.keyword()));
            }
            if (request.category() != null) {
                Join<Place, Category> categories = root.join("categories");
                predicates.add(criteriaBuilder.equal(categories.get("slug"), request.category()));
                query.distinct(true);
            }
            if (request.indoor() != null) {
                predicates.add(criteriaBuilder.equal(root.get("indoor"), request.indoor()));
            }
            if (request.maxCost() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("minCost"), request.maxCost()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Place> adminKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            Expression<String> pattern = normalizedPattern(criteriaBuilder, keyword);
            return criteriaBuilder.or(
                    literalSubstring(criteriaBuilder, root.get("name"), pattern),
                    literalSubstring(criteriaBuilder, root.get("slug"), pattern),
                    literalSubstring(criteriaBuilder, root.get("address"), pattern));
        };
    }

    private static Predicate keywordPredicate(Root<Place> root, CriteriaBuilder criteriaBuilder, String keyword) {
        Expression<String> pattern = normalizedPattern(criteriaBuilder, keyword);

        return criteriaBuilder.or(
                literalSubstring(criteriaBuilder, root.get("name"), pattern),
                literalSubstring(criteriaBuilder, root.get("shortDescription"), pattern),
                literalSubstring(criteriaBuilder, root.get("fullDescription"), pattern),
                literalSubstring(criteriaBuilder, root.get("address"), pattern));
    }

    private static Expression<String> normalizedPattern(CriteriaBuilder criteriaBuilder, String keyword) {
        String escapedKeyword = PlaceSearchNormalizer.escapeLikePattern(keyword.toLowerCase(Locale.ROOT));
        return normalizedLiteral(criteriaBuilder, "%" + escapedKeyword + "%");
    }

    private static Predicate literalSubstring(
            CriteriaBuilder criteriaBuilder, Expression<String> column, Expression<String> pattern) {
        return criteriaBuilder.like(
                normalizedColumn(criteriaBuilder, column), pattern, PlaceSearchNormalizer.LIKE_ESCAPE_CHARACTER);
    }

    private static Expression<String> normalizedColumn(CriteriaBuilder criteriaBuilder, Expression<String> column) {
        return criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(column));
    }

    private static Expression<String> normalizedLiteral(CriteriaBuilder criteriaBuilder, String value) {
        HibernateCriteriaBuilder hibernateCriteriaBuilder = (HibernateCriteriaBuilder) criteriaBuilder;
        return criteriaBuilder.function("unaccent", String.class, hibernateCriteriaBuilder.value(value));
    }
}
