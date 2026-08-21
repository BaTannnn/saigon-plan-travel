import { requestJson } from "@/lib/api/api-client";
import type {
  PlaceDetail,
  PlacePage,
  PlacesSearchFilters,
} from "@/types/place";

function appendIfPresent(
  params: URLSearchParams,
  key: string,
  value: string | number | undefined,
) {
  if (value === undefined || String(value).trim() === "") {
    return;
  }

  params.set(key, String(value));
}

export async function getPlaces(filters: PlacesSearchFilters) {
  const params = new URLSearchParams();
  appendIfPresent(params, "keyword", filters.keyword);
  appendIfPresent(params, "category", filters.category);
  appendIfPresent(params, "indoor", filters.indoor);
  appendIfPresent(params, "maxCost", filters.maxCost);
  appendIfPresent(params, "page", filters.page);
  appendIfPresent(params, "size", filters.size);

  return requestJson<PlacePage>(`/api/v1/places?${params.toString()}`, {
    method: "GET",
    cache: "no-store",
  });
}

export async function getPlaceDetail(slug: string) {
  return requestJson<PlaceDetail>(
    `/api/v1/places/${encodeURIComponent(slug)}`,
    {
      method: "GET",
      cache: "no-store",
    },
  );
}
