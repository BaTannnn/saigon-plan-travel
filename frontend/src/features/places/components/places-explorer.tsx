"use client";

import { useState } from "react";
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
import { Pagination } from "@/features/places/components/pagination";
import { PlaceList } from "@/features/places/components/place-list";
import { SearchForm } from "@/features/places/components/search-form";
import { MapShell } from "@/features/map/map-shell";
import type { Category, PlacePage, PlacesSearchFilters } from "@/types/place";

type PlacesExplorerProps = {
  result: PlacePage;
  categories: Category[];
  districts: string[];
  filters: PlacesSearchFilters;
};

export function PlacesExplorer({
  result,
  categories,
  districts,
  filters,
}: PlacesExplorerProps) {
  const [selectedSlug, setSelectedSlug] = useState<string | null>(
    result.content[0]?.slug ?? null,
  );
  const [mobileView, setMobileView] = useState<"list" | "map">("list");
  const filterPanelKey = [
    filters.district,
    filters.category,
    filters.indoor,
    filters.maxCost,
  ].join("|");

  const effectiveSelectedSlug = result.content.some(
    (place) => place.slug === selectedSlug,
  )
    ? selectedSlug
    : result.content[0]?.slug ?? null;

  function selectPlace(slug: string) {
    setSelectedSlug(slug);
    if (window.matchMedia("(max-width: 767px)").matches) {
      setMobileView("map");
    }
  }

  return (
    <main className="places-explorer">
      <section className={mobileView === "map" ? "places-sidebar mobile-hidden" : "places-sidebar"}>
        <div className="sidebar-intro">
          <p className="eyebrow">Khám phá Sài Gòn</p>
          <h1>Chào Tân, hôm nay muốn khám phá đâu?</h1>
          <Button
            className="create-trip-button"
            type="button"
            variant="accent"
            size="lg"
            disabled
          >
            <CalendarIcon />
            Tạo lịch trình
            <span>Sắp ra mắt</span>
          </Button>
          <div className="mobile-search"><SearchForm filters={filters} /></div>
          <CategoryChips categories={categories} filters={filters} />
        </div>

        <div className="desktop-filters">
          <FilterPanel
            key={`desktop-${filterPanelKey}`}
            categories={categories}
            districts={districts}
            filters={filters}
          />
        </div>

        <div className="results-heading">
          <div>
            <p className="eyebrow">Địa điểm nổi bật</p>
            <h2>{result.totalElements} kết quả</h2>
          </div>
          <Sheet>
            <SheetTrigger asChild>
              <Button className="filter-trigger" type="button" variant="outline">
                <FilterIcon /> Lọc
              </Button>
            </SheetTrigger>
            <SheetContent className="filter-sheet-content" side="bottom">
              <SheetHeader className="sr-only">
                <SheetTitle>Bộ lọc địa điểm</SheetTitle>
                <SheetDescription>
                  Chọn khu vực, danh mục, không gian hoặc mức chi phí.
                </SheetDescription>
              </SheetHeader>
              <FilterPanel
                key={`mobile-${filterPanelKey}`}
                categories={categories}
                districts={districts}
                filters={filters}
              />
            </SheetContent>
          </Sheet>
        </div>

        <PlaceList places={result.content} selectedSlug={effectiveSelectedSlug} onSelect={selectPlace} />
        <Pagination page={result} filters={filters} />
      </section>

      <section className={mobileView === "list" ? "map-pane mobile-hidden" : "map-pane"} aria-label="Bản đồ địa điểm">
        <div className="map-search"><SearchForm filters={filters} compact /></div>
        <MapShell places={result.content} selectedSlug={effectiveSelectedSlug} onSelect={setSelectedSlug} />
      </section>

      <div className="mobile-view-switcher" aria-label="Chọn kiểu hiển thị">
        <Button
          type="button"
          className={mobileView === "list" ? "active" : ""}
          onClick={() => setMobileView("list")}
          aria-pressed={mobileView === "list"}
          aria-label="Hiển thị danh sách địa điểm"
          variant="ghost"
        >
          <LayersIcon /> Danh sách
        </Button>
        <Button
          type="button"
          className={mobileView === "map" ? "active" : ""}
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
