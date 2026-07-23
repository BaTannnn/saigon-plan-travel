import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Pagination as PaginationRoot,
  PaginationContent,
  PaginationItem,
} from "@/components/ui/pagination";
import { createPlacesHref } from "@/features/places/search-params";
import type { PlacePage, PlacesSearchFilters } from "@/types/place";

type PaginationProps = {
  page: PlacePage;
  filters: PlacesSearchFilters;
};

export function Pagination({ page, filters }: PaginationProps) {
  if (page.totalPages <= 1) return null;

  return (
    <PaginationRoot
      className="pt-3 text-[0.78rem] font-bold max-md:py-4"
      aria-label="Phân trang địa điểm"
    >
      <PaginationContent className="w-full justify-between gap-2.5">
        <PaginationItem>
          {page.first ? (
            <Button
              className="min-h-[38px] rounded-[10px] px-3 text-primary"
              type="button"
              variant="outline"
              disabled
            >
              Trước
            </Button>
          ) : (
            <Button
              asChild
              className="min-h-[38px] rounded-[10px] px-3 text-primary"
              variant="outline"
            >
              <Link href={createPlacesHref(filters, { page: page.page - 1 })}>
                Trước
              </Link>
            </Button>
          )}
        </PaginationItem>
        <PaginationItem>
          <span className="whitespace-nowrap" aria-current="page">
            Trang {page.page + 1} / {page.totalPages}
          </span>
        </PaginationItem>
        <PaginationItem>
          {page.last ? (
            <Button
              className="min-h-[38px] rounded-[10px] px-3 text-primary"
              type="button"
              variant="outline"
              disabled
            >
              Sau
            </Button>
          ) : (
            <Button
              asChild
              className="min-h-[38px] rounded-[10px] px-3 text-primary"
              variant="outline"
            >
              <Link href={createPlacesHref(filters, { page: page.page + 1 })}>
                Sau
              </Link>
            </Button>
          )}
        </PaginationItem>
      </PaginationContent>
    </PaginationRoot>
  );
}
