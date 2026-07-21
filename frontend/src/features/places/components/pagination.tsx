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
    <PaginationRoot className="pagination" aria-label="Phân trang địa điểm">
      <PaginationContent className="pagination-content">
        <PaginationItem>
          {page.first ? (
            <Button className="pagination-link" type="button" variant="outline" disabled>
              Trước
            </Button>
          ) : (
            <Button asChild className="pagination-link" variant="outline">
              <Link href={createPlacesHref(filters, { page: page.page - 1 })}>
                Trước
              </Link>
            </Button>
          )}
        </PaginationItem>
        <PaginationItem>
          <span className="pagination-status" aria-current="page">
            Trang {page.page + 1} / {page.totalPages}
          </span>
        </PaginationItem>
        <PaginationItem>
          {page.last ? (
            <Button className="pagination-link" type="button" variant="outline" disabled>
              Sau
            </Button>
          ) : (
            <Button asChild className="pagination-link" variant="outline">
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
