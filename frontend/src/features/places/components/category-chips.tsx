import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { createPlacesHref } from "@/features/places/search-params";
import type { Category, PlacesSearchFilters } from "@/types/place";

type CategoryChipsProps = {
  categories: Category[];
  filters: PlacesSearchFilters;
};

export function CategoryChips({ categories, filters }: CategoryChipsProps) {
  return (
    <nav className="category-chips" aria-label="Lọc theo danh mục">
      <Badge
        asChild
        className={!filters.category ? "category-chip active" : "category-chip"}
        variant="outline"
      >
        <Link href={createPlacesHref(filters, { category: undefined, page: 0 })}>
          Tất cả
        </Link>
      </Badge>
      {categories.map((category) => (
        <Badge
          asChild
          className={
            filters.category === category.slug
              ? "category-chip active"
              : "category-chip"
          }
          key={category.id}
          variant="outline"
        >
          <Link
            href={createPlacesHref(filters, {
              category:
                filters.category === category.slug ? undefined : category.slug,
              page: 0,
            })}
          >
            {category.name}
          </Link>
        </Badge>
      ))}
    </nav>
  );
}
