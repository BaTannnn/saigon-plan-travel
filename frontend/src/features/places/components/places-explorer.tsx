"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { CalendarIcon, FilterIcon, LayersIcon } from "@/components/ui/icons";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { CategoryChips } from "@/features/places/components/category-chips";
import { FilterPanel } from "@/features/places/components/filter-panel";
import { PlaceList } from "@/features/places/components/place-list";
import { PlacesPagination } from "@/features/places/components/places-pagination";
import { SearchForm } from "@/features/places/components/search-form";
import { MapShell } from "@/features/places/map/map-shell";
import { cn } from "@/lib/utils";
import type { Category } from "@/types/category";
import type { PlacePage, PlacesSearchFilters } from "@/types/place";

type PlacesExplorerProps = {
  result: PlacePage;
  categories: Category[];
  filters: PlacesSearchFilters;
};

const eyebrowClassName =
  "m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase";

export function PlacesExplorer({
  result,
  categories,
  filters,
}: PlacesExplorerProps) {
  const [selectedSlug, setSelectedSlug] = useState<string | null>(
    result.content[0]?.slug ?? null,
  );
  const [mobileView, setMobileView] = useState<"list" | "map">("list");
  const filterPanelKey = [
    filters.category,
    filters.indoor,
    filters.maxCost,
  ].join("|");

  const effectiveSelectedSlug = result.content.some(
    (place) => place.slug === selectedSlug,
  )
    ? selectedSlug
    : (result.content[0]?.slug ?? null);

  function selectPlace(slug: string) {
    setSelectedSlug(slug);
    if (window.matchMedia("(max-width: 767px)").matches) {
      setMobileView("map");
    }
  }

  return (
    <main className="grid h-[calc(100dvh_-_5rem)] min-h-[620px] grid-cols-[minmax(410px,37%)_1fr] overflow-hidden md:max-[1100px]:grid-cols-[minmax(370px,42%)_1fr] max-md:block max-md:h-auto max-md:min-h-[calc(100dvh_-_4rem)] max-md:overflow-visible">
      <section
        className={cn(
          "relative z-[2] flex min-w-0 flex-col overflow-hidden border-r border-border bg-[linear-gradient(145deg,var(--surface),var(--background))] px-7 pt-7 pb-5 md:max-[1100px]:px-[18px] md:max-[1100px]:pt-[22px] md:max-[1100px]:pb-[18px] max-md:min-h-[calc(100dvh_-_4rem)] max-md:overflow-visible max-md:border-r-0 max-md:px-4 max-md:pt-5 max-md:pb-[92px]",
          mobileView === "map" && "max-md:hidden",
        )}
      >
        <div>
          <p className={eyebrowClassName}>Khám phá Sài Gòn</p>
          <h1 className="mt-1 mb-[18px] max-w-[520px] text-[clamp(1.65rem,2.2vw,2.35rem)] leading-[1.16] font-bold tracking-[-0.045em] max-md:mb-3.5 max-md:text-[1.65rem]">
            Hôm nay bạn muốn khám phá đâu?
          </h1>
          <Button
            asChild
            className="w-full gap-2.5"
            variant="accent"
            size="lg"
          >
            <Link href="/trips/new">
              <CalendarIcon />
              Tạo chuyến đi
            </Link>
          </Button>
          <div className="mt-3 hidden max-md:block">
            <SearchForm filters={filters} />
          </div>
          <CategoryChips categories={categories} filters={filters} />
        </div>

        <div className="my-3 md:max-[1100px]:max-h-[205px] md:max-[1100px]:overflow-y-auto max-md:hidden">
          <FilterPanel
            key={`desktop-${filterPanelKey}`}
            categories={categories}
            filters={filters}
          />
        </div>

        <div className="mt-1 mb-3 flex items-center justify-between">
          <div>
            <p className={eyebrowClassName}>Địa điểm nổi bật</p>
            <h2 className="mt-0.5 text-lg font-bold">
              {result.totalElements} kết quả
            </h2>
          </div>
          <Sheet>
            <SheetTrigger asChild>
              <Button
                className="hidden gap-[7px] px-3.5 text-primary-strong max-md:inline-flex"
                type="button"
                variant="outline"
              >
                <FilterIcon /> Lọc
              </Button>
            </SheetTrigger>
            <SheetContent
              className="max-h-[calc(100dvh_-_48px)] overflow-y-auto rounded-t-mint-lg border-border bg-background p-2 shadow-mint-md"
              side="bottom"
            >
              <SheetHeader className="sr-only">
                <SheetTitle>Bộ lọc địa điểm</SheetTitle>
                <SheetDescription>
                  Chọn khu vực, danh mục, không gian hoặc mức chi phí.
                </SheetDescription>
              </SheetHeader>
              <FilterPanel
                className="border-0 shadow-none [&_[data-slot=button]]:min-h-12 [&_[data-slot=input]]:h-12 [&_[data-slot=select-trigger]]:h-12"
                key={`mobile-${filterPanelKey}`}
                categories={categories}
                filters={filters}
              />
            </SheetContent>
          </Sheet>
        </div>

        <PlaceList
          places={result.content}
          selectedSlug={effectiveSelectedSlug}
          onSelect={selectPlace}
        />
        <PlacesPagination page={result} filters={filters} />
      </section>

      <section
        className={cn(
          "relative min-w-0 overflow-hidden bg-primary-soft max-md:h-[calc(100dvh_-_4rem)] max-md:min-h-[420px] max-md:pb-[72px]",
          mobileView === "list" && "max-md:hidden",
        )}
        aria-label="Bản đồ địa điểm"
      >
        <div className="absolute top-6 right-6 z-[500] w-[min(390px,calc(100%_-_48px))] max-md:hidden">
          <SearchForm filters={filters} compact />
        </div>
        <MapShell
          places={result.content}
          selectedSlug={effectiveSelectedSlug}
          onSelect={setSelectedSlug}
        />
      </section>

      <div
        className="fixed inset-x-4 bottom-4 z-[900] hidden grid-cols-2 gap-1 rounded-[15px] border border-border bg-surface p-1 shadow-mint-md max-md:grid"
        aria-label="Chọn kiểu hiển thị"
      >
        <Button
          type="button"
          className={cn(
            "min-h-12 gap-[7px] rounded-[11px] font-extrabold text-text-secondary",
            mobileView === "list" &&
              "bg-primary text-surface hover:bg-primary/90 hover:text-surface",
          )}
          onClick={() => setMobileView("list")}
          aria-pressed={mobileView === "list"}
          aria-label="Hiển thị danh sách địa điểm"
          variant="ghost"
        >
          <LayersIcon /> Danh sách
        </Button>
        <Button
          type="button"
          className={cn(
            "min-h-12 gap-[7px] rounded-[11px] font-extrabold text-text-secondary",
            mobileView === "map" &&
              "bg-primary text-surface hover:bg-primary/90 hover:text-surface",
          )}
          onClick={() => setMobileView("map")}
          aria-pressed={mobileView === "map"}
          aria-label="Hiển thị bản đồ địa điểm"
          variant="ghost"
        >
          <span aria-hidden="true">⌖</span> Bản đồ
        </Button>
      </div>
    </main>
  );
}
