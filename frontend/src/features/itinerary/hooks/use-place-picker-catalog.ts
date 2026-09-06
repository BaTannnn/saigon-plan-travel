import { useEffect, useMemo, useRef, useState } from "react";
import { getCategories } from "@/lib/api/category-api";
import { getPlaces } from "@/lib/api/place-api";
import type { Category } from "@/types/category";
import type { PlaceSummary, PlacesSearchFilters } from "@/types/place";

type PickerFilters = Pick<
  PlacesSearchFilters,
  "keyword" | "category" | "indoor" | "maxCost"
>;

export const PLACE_PICKER_ALL_VALUE = "__all__";

const PAGE_SIZE = 10;
const initialFilters: PickerFilters = {};

export function usePlacePickerCatalog(
  open: boolean,
  excludedPlaceSlugs: string[],
) {
  const [places, setPlaces] = useState<PlaceSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState(PLACE_PICKER_ALL_VALUE);
  const [indoor, setIndoor] = useState(PLACE_PICKER_ALL_VALUE);
  const [maxCost, setMaxCost] = useState("");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [initialLoaded, setInitialLoaded] = useState(false);
  const [filterLoading, setFilterLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const categoriesLoadedRef = useRef(false);

  const currentFilters: PickerFilters = {
    keyword: query.trim() || undefined,
    category:
      category === PLACE_PICKER_ALL_VALUE ? undefined : category,
    indoor:
      indoor === PLACE_PICKER_ALL_VALUE
        ? undefined
        : (indoor as "true" | "false"),
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

  function search() {
    return loadPage(0, false);
  }

  function resetFilters() {
    setQuery("");
    setCategory(PLACE_PICKER_ALL_VALUE);
    setIndoor(PLACE_PICKER_ALL_VALUE);
    setMaxCost("");
    return loadPage(0, false, initialFilters);
  }

  function loadMore() {
    return loadPage(page + 1, true);
  }

  function reset() {
    setQuery("");
    setCategory(PLACE_PICKER_ALL_VALUE);
    setIndoor(PLACE_PICKER_ALL_VALUE);
    setMaxCost("");
    setPlaces([]);
    setPage(0);
    setHasMore(false);
    setError(null);
    setInitialLoaded(false);
    setFilterLoading(false);
    setLoadingMore(false);
  }

  return {
    categories,
    visiblePlaces,
    query,
    setQuery,
    category,
    setCategory,
    indoor,
    setIndoor,
    maxCost,
    setMaxCost,
    hasMore,
    initialLoading,
    filterLoading,
    loadingMore,
    error,
    search,
    resetFilters,
    loadMore,
    reset,
  };
}
