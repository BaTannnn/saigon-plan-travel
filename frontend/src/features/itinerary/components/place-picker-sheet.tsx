"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { Filter, MapPin, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { getCategories } from "@/lib/api/category-api";
import { getPlaceCatalog, getPlaces } from "@/lib/api/place-api";
import type { Category } from "@/types/category";
import type { PlaceSummary, PlacesSearchFilters } from "@/types/place";

export type PlacePickerMode = "add" | "replace";

type PlacePickerSheetProps = {
  open: boolean;
  mode: PlacePickerMode;
  excludedPlaceSlugs: string[];
  busy?: boolean;
  onOpenChange: (open: boolean) => void;
  onSelect: (place: PlaceSummary) => Promise<void>;
};

const ALL_VALUE = "__all__";

type CatalogLoadResult = [
  Awaited<ReturnType<typeof getPlaceCatalog>>,
  Awaited<ReturnType<typeof getCategories>>,
];

export function PlacePickerSheet({
  open,
  mode,
  excludedPlaceSlugs,
  busy = false,
  onOpenChange,
  onSelect,
}: PlacePickerSheetProps) {
  const [places, setPlaces] = useState<PlaceSummary[]>([]);
  const [catalog, setCatalog] = useState<PlaceSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState(ALL_VALUE);
  const [indoor, setIndoor] = useState(ALL_VALUE);
  const [maxCost, setMaxCost] = useState("");
  const [catalogLoaded, setCatalogLoaded] = useState(false);
  const [filterLoading, setFilterLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const catalogRequestRef = useRef<Promise<CatalogLoadResult> | null>(null);
  const catalogLoading = open && !catalogLoaded && error === null;
  const loading = catalogLoading || filterLoading;

  useEffect(() => {
    if (!open || catalogLoaded) return;

    let cancelled = false;
    const request =
      catalogRequestRef.current ??
      Promise.all([getPlaceCatalog(), getCategories()]);
    catalogRequestRef.current = request;

    request
      .then(([placeCatalog, categoryList]) => {
        if (cancelled) return;

        setCatalog(placeCatalog.content);
        setPlaces(placeCatalog.content);
        setCategories(categoryList);
        setCatalogLoaded(true);
      })
      .catch(() => {
        if (cancelled) return;

        setError("Không thể tải dữ liệu địa điểm. Vui lòng thử lại.");
      })
      .finally(() => {
        if (catalogRequestRef.current === request) {
          catalogRequestRef.current = null;
        }
      });

    return () => {
      cancelled = true;
    };
  }, [catalogLoaded, open]);

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      setQuery("");
      setCategory(ALL_VALUE);
      setIndoor(ALL_VALUE);
      setMaxCost("");
      setPlaces(catalog);
      setSelectedPlaceId(null);
      setError(null);
    }

    onOpenChange(nextOpen);
  }

  const visiblePlaces = useMemo(() => {
    const excluded = new Set(excludedPlaceSlugs);
    return places.filter((place) => !excluded.has(place.slug));
  }, [excludedPlaceSlugs, places]);

  async function applyFilters(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    setFilterLoading(true);
    setError(null);

    const filters: PlacesSearchFilters = {
      keyword: query.trim() || undefined,
      category: category === ALL_VALUE ? undefined : category,
      indoor:
        indoor === ALL_VALUE ? undefined : (indoor as "true" | "false"),
      maxCost: maxCost.trim() || undefined,
      page: 0,
      size: 100,
    };

    try {
      const response = await getPlaces(filters);
      setPlaces(response.content);
    } catch {
      setError("Không thể áp dụng bộ lọc. Vui lòng thử lại.");
    } finally {
      setFilterLoading(false);
    }
  }

  function resetFilters() {
    setQuery("");
    setCategory(ALL_VALUE);
    setIndoor(ALL_VALUE);
    setMaxCost("");
    setPlaces(catalog);
    setError(null);
  }

  async function handleSelect(place: PlaceSummary) {
    setSelectedPlaceId(place.id);
    setError(null);

    try {
      await onSelect(place);
      handleOpenChange(false);
    } catch {
      setError(
        mode === "add"
          ? "Không thể thêm địa điểm vào hành trình."
          : "Không thể thay địa điểm trong hành trình.",
      );
    } finally {
      setSelectedPlaceId(null);
    }
  }

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetContent className="w-[min(94vw,520px)] sm:max-w-[520px]">
        <SheetHeader className="border-b border-border px-5 py-5 pr-14">
          <SheetTitle className="text-lg font-bold">
            {mode === "add" ? "Thêm địa điểm" : "Thay địa điểm"}
          </SheetTitle>
          <SheetDescription>
            Tìm kiếm và lọc địa điểm ngay trong quá trình xây dựng hành trình.
          </SheetDescription>
        </SheetHeader>

        <form className="grid gap-3 px-5" onSubmit={applyFilters}>
          <div className="relative">
            <Search
              className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-text-secondary"
              aria-hidden="true"
            />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="pl-9"
              placeholder="Tìm theo tên hoặc mô tả..."
              aria-label="Tìm địa điểm"
            />
          </div>

          <div className="rounded-xl border border-border bg-background p-3">
            <div className="mb-3 flex items-center gap-2 text-sm font-bold text-text-primary">
              <Filter className="size-4 text-primary" aria-hidden="true" />
              Bộ lọc
            </div>

            <div className="grid grid-cols-1 gap-3">
              <div className="grid gap-1.5">
                <Label className="text-xs text-text-secondary">Danh mục</Label>
                <Select value={category} onValueChange={setCategory}>
                  <SelectTrigger className="h-10 w-full bg-surface">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent position="popper">
                    <SelectItem value={ALL_VALUE}>Tất cả danh mục</SelectItem>
                    {categories.map((item) => (
                      <SelectItem key={item.id} value={item.slug}>
                        {item.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="grid gap-1.5">
                <Label className="text-xs text-text-secondary">Không gian</Label>
                <Select value={indoor} onValueChange={setIndoor}>
                  <SelectTrigger className="h-10 w-full bg-surface">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent position="popper">
                    <SelectItem value={ALL_VALUE}>
                      Trong nhà & ngoài trời
                    </SelectItem>
                    <SelectItem value="true">Trong nhà</SelectItem>
                    <SelectItem value="false">Ngoài trời</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="grid gap-1.5">
                <Label
                  className="text-xs text-text-secondary"
                  htmlFor="picker-max-cost"
                >
                  Chi phí tối đa (VND)
                </Label>
                <Input
                  id="picker-max-cost"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={maxCost}
                  onChange={(event) => setMaxCost(event.target.value)}
                  placeholder="Ví dụ: 200000"
                  className="h-10 bg-surface"
                />
              </div>
            </div>

            <div className="mt-3 flex gap-2">
              <Button type="submit" size="sm" disabled={loading}>
                {loading ? "Đang lọc..." : "Áp dụng"}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={resetFilters}
                disabled={loading}
              >
                Đặt lại
              </Button>
            </div>
          </div>
        </form>

        {error ? (
          <p className="mx-5 rounded-lg bg-destructive/10 px-3 py-2 text-sm font-semibold text-destructive">
            {error}
          </p>
        ) : null}

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5">
          <div className="mb-2 flex items-center justify-between text-xs text-text-secondary">
            <span>
              {loading ? "Đang tải..." : `${visiblePlaces.length} địa điểm`}
            </span>
          </div>

          {loading ? (
            <div className="grid gap-3 py-2" aria-label="Đang tải địa điểm">
              {[1, 2, 3, 4].map((item) => (
                <div
                  key={item}
                  className="h-20 animate-pulse rounded-xl bg-muted/70"
                />
              ))}
            </div>
          ) : visiblePlaces.length === 0 ? (
            <div className="grid min-h-44 place-items-center text-center">
              <div>
                <p className="font-bold text-text-primary">
                  Không tìm thấy địa điểm phù hợp
                </p>
                <p className="mt-1 text-sm text-text-secondary">
                  Hãy thử thay đổi từ khóa hoặc bộ lọc.
                </p>
              </div>
            </div>
          ) : (
            <div className="grid gap-2 py-2">
              {visiblePlaces.map((place) => {
                const selecting = selectedPlaceId === place.id || busy;

                return (
                  <div
                    key={place.id}
                    className="grid grid-cols-[1fr_auto] items-center gap-3 rounded-xl border border-border bg-surface p-3"
                  >
                    <div className="min-w-0">
                      <div className="flex items-start gap-2">
                        <MapPin
                          className="mt-0.5 size-4 shrink-0 text-primary"
                          aria-hidden="true"
                        />
                        <div className="min-w-0">
                          <p className="m-0 truncate font-bold text-text-primary">
                            {place.name}
                          </p>
                          <p className="mt-1 mb-0 truncate text-xs text-text-secondary">
                            {place.indoor ? "Trong nhà" : "Ngoài trời"}
                          </p>
                          {place.shortDescription ? (
                            <p className="mt-1.5 mb-0 line-clamp-2 text-xs leading-5 text-text-secondary">
                              {place.shortDescription}
                            </p>
                          ) : null}
                        </div>
                      </div>
                    </div>

                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={selecting}
                      onClick={() => handleSelect(place)}
                    >
                      {selectedPlaceId === place.id
                        ? "Đang lưu..."
                        : mode === "add"
                          ? "Thêm"
                          : "Chọn"}
                    </Button>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
