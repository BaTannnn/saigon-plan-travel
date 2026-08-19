"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { Filter, MapPin, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { getCategories } from "@/lib/api/category-api";
import { getPlaces } from "@/lib/api/place-api";
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

type PickerFilters = Pick<
  PlacesSearchFilters,
  "keyword" | "category" | "indoor" | "maxCost"
>;

const ALL_VALUE = "__all__";
const PAGE_SIZE = 10;
const initialFilters: PickerFilters = {};

export function PlacePickerSheet({
  open,
  mode,
  excludedPlaceSlugs,
  busy = false,
  onOpenChange,
  onSelect,
}: PlacePickerSheetProps) {
  const [places, setPlaces] = useState<PlaceSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState(ALL_VALUE);
  const [indoor, setIndoor] = useState(ALL_VALUE);
  const [maxCost, setMaxCost] = useState("");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [initialLoaded, setInitialLoaded] = useState(false);
  const [filterLoading, setFilterLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const categoriesLoadedRef = useRef(false);

  const currentFilters: PickerFilters = {
    keyword: query.trim() || undefined,
    category: category === ALL_VALUE ? undefined : category,
    indoor: indoor === ALL_VALUE ? undefined : (indoor as "true" | "false"),
    maxCost: maxCost.trim() || undefined,
  };

  const visiblePlaces = useMemo(() => {
    const excluded = new Set(excludedPlaceSlugs);
    return places.filter((place) => !excluded.has(place.slug));
  }, [excludedPlaceSlugs, places]);
  const initialLoading = !initialLoaded && error === null;

  useEffect(() => {
    if (!open) return;

    let cancelled = false;
    getPlaces({ ...initialFilters, page: 0, size: PAGE_SIZE })
      .then((response) => {
        if (cancelled) return;
        setPlaces(response.content);
        setPage(response.page);
        setHasMore(!response.last);
      })
      .catch(() => {
        if (!cancelled) setError("Không thể tải địa điểm. Vui lòng thử lại.");
      })
      .finally(() => {
        if (!cancelled) setInitialLoaded(true);
      });

    if (!categoriesLoadedRef.current) {
      void getCategories()
        .then((categoryList) => {
          if (!cancelled) {
            setCategories(categoryList);
            categoriesLoadedRef.current = true;
          }
        })
        .catch(() => {
          if (!cancelled) {
            setError("Không thể tải bộ lọc địa điểm. Vui lòng thử lại.");
          }
        });
    }

    return () => {
      cancelled = true;
    };
  }, [open]);

  function filtersForPage(nextPage: number, filters = currentFilters) {
    return { ...filters, page: nextPage, size: PAGE_SIZE };
  }

  async function loadPage(
    nextPage: number,
    append: boolean,
    filters = currentFilters,
  ) {
    if (append) setLoadingMore(true);
    else setFilterLoading(true);
    setError(null);

    try {
      const response = await getPlaces(filtersForPage(nextPage, filters));
      setPlaces((current) =>
        append ? [...current, ...response.content] : response.content,
      );
      setPage(response.page);
      setHasMore(!response.last);
    } catch {
      setError(
        append
          ? "Không thể tải thêm địa điểm. Vui lòng thử lại."
          : "Không thể tìm địa điểm. Vui lòng thử lại.",
      );
    } finally {
      if (append) setLoadingMore(false);
      else setFilterLoading(false);
    }
  }

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      setQuery("");
      setCategory(ALL_VALUE);
      setIndoor(ALL_VALUE);
      setMaxCost("");
      setPlaces([]);
      setPage(0);
      setHasMore(false);
      setError(null);
      setSelectedPlaceId(null);
      setInitialLoaded(false);
      setFilterLoading(false);
    }

    onOpenChange(nextOpen);
  }

  function applyFilters(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    void loadPage(0, false);
  }

  function resetFilters() {
    setQuery("");
    setCategory(ALL_VALUE);
    setIndoor(ALL_VALUE);
    setMaxCost("");
    void loadPage(0, false, initialFilters);
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
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="w-[min(calc(100vw_-_2rem),560px)]">
        <DialogHeader className="border-b border-border pb-5">
          <DialogTitle>
            {mode === "add" ? "Thêm địa điểm" : "Thay địa điểm"}
          </DialogTitle>
          <DialogDescription>
            Tìm địa điểm phù hợp ngay trong hành trình của bạn.
          </DialogDescription>
        </DialogHeader>

        <form className="px-5 pt-5" onSubmit={applyFilters}>
          <div className="relative">
            <Search
              className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-text-secondary"
              aria-hidden="true"
            />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="h-11 pl-9"
              placeholder="Tìm tên địa điểm..."
              aria-label="Tìm địa điểm"
            />
          </div>

          <details className="mt-3 border-b border-border pb-3">
            <summary className="flex cursor-pointer list-none items-center gap-2 text-sm font-semibold text-text-secondary hover:text-text-primary">
              <Filter className="size-4 text-primary" aria-hidden="true" />
              Bộ lọc
            </summary>
            <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
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

              <div className="grid gap-1.5 sm:col-span-2">
                <Label className="text-xs text-text-secondary" htmlFor="picker-max-cost">
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
            <div className="mt-3 flex justify-end gap-2">
              <Button type="button" size="sm" variant="ghost" onClick={resetFilters}>
                Đặt lại
              </Button>
              <Button type="submit" size="sm" disabled={initialLoading || filterLoading}>
                Áp dụng
              </Button>
            </div>
          </details>
        </form>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5">
          <div className="flex min-h-9 items-center justify-between text-xs text-text-secondary">
            <span>{initialLoading || filterLoading ? "Đang tải..." : `${visiblePlaces.length} địa điểm`}</span>
          </div>

          {error ? (
            <div className="mb-3 flex items-center justify-between gap-3 border-l-2 border-destructive bg-destructive/8 px-3 py-2 text-sm text-destructive" role="alert">
              <span>{error}</span>
              <Button type="button" size="xs" variant="ghost" onClick={() => applyFilters()}>
                Thử lại
              </Button>
            </div>
          ) : null}

          {initialLoading && places.length === 0 ? (
            <div className="grid gap-0" aria-label="Đang tải địa điểm">
              {[1, 2, 3, 4].map((item) => (
                <div key={item} className="h-18 animate-pulse border-b border-border bg-muted/45" />
              ))}
            </div>
          ) : visiblePlaces.length === 0 ? (
            <div className="grid min-h-40 place-items-center text-center">
              <div>
                <p className="font-bold text-text-primary">Không tìm thấy địa điểm phù hợp</p>
                <p className="mt-1 text-sm text-text-secondary">
                  Không có địa điểm nào trong SaigonPlanTravel khớp với tìm kiếm này.
                </p>
              </div>
            </div>
          ) : (
            <div className="border-y border-border">
              {visiblePlaces.map((place) => {
                const selecting = selectedPlaceId === place.id || busy;

                return (
                  <div key={place.id} className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 border-b border-border py-3 last:border-b-0">
                    <div className="flex min-w-0 items-start gap-2">
                      <MapPin className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
                      <div className="min-w-0">
                        <p className="m-0 truncate text-sm font-bold text-text-primary">{place.name}</p>
                        <p className="mt-0.5 mb-0 text-xs text-text-secondary">
                          {place.indoor ? "Trong nhà" : "Ngoài trời"}
                        </p>
                        {place.shortDescription ? (
                          <p className="mt-1 mb-0 line-clamp-1 text-xs leading-5 text-text-secondary">
                            {place.shortDescription}
                          </p>
                        ) : null}
                      </div>
                    </div>

                    <Button type="button" size="sm" variant="outline" disabled={selecting} onClick={() => handleSelect(place)}>
                      {selectedPlaceId === place.id ? "Đang lưu..." : mode === "add" ? "Thêm" : "Chọn"}
                    </Button>
                  </div>
                );
              })}
            </div>
          )}

          {hasMore && !initialLoading ? (
            <div className="pt-4 text-center">
              <Button type="button" size="sm" variant="outline" onClick={() => void loadPage(page + 1, true)} disabled={loadingMore || busy}>
                {loadingMore ? "Đang tải thêm..." : "Xem thêm"}
              </Button>
            </div>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  );
}
