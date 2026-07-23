import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { createPlacesHref } from "@/features/places/search-params";
import { cn } from "@/lib/utils";
import type { Category, PlacesSearchFilters } from "@/types/place";

type CategoryChipsProps = {
  categories: Category[];
  filters: PlacesSearchFilters;
};

export function CategoryChips({ categories, filters }: CategoryChipsProps) {
  const chipClassName =
    "h-10 shrink-0 rounded-full border-border bg-surface px-3.5 text-[0.82rem] font-bold text-primary-strong hover:border-[color-mix(in_srgb,var(--primary)_42%,var(--border))] hover:bg-primary-soft";

  return (
    <nav
      className="flex gap-2 overflow-x-auto px-px pt-4 pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      aria-label="Lọc theo danh mục"
    >
      <Badge
        asChild
        className={cn(
          chipClassName,
          !filters.category &&
            "border-[color-mix(in_srgb,var(--primary)_42%,var(--border))] bg-primary-soft",
        )}
        variant="outline"
      >
        <Link
          href={createPlacesHref(filters, { category: undefined, page: 0 })}
        >
          Tất cả
        </Link>
      </Badge>
      {categories.map((category) => (
        <Badge
          asChild
          className={cn(
            chipClassName,
            filters.category === category.slug &&
              "border-[color-mix(in_srgb,var(--primary)_42%,var(--border))] bg-primary-soft",
          )}
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
