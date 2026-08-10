import { requestJson } from "@/lib/api/api-client";
import type {
  PlaceDetail,
  PlacePage,
  PlacesSearchFilters,
} from "@/types/place";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

function getBackendBaseUrl() {
  return (process.env.BACKEND_API_BASE_URL ?? DEFAULT_BACKEND_URL).replace(
    /\/$/,
    "",
  );
}

function requestPlaceJson<T>(path: string) {
  return requestJson<T>(`${getBackendBaseUrl()}${path}`, {
    cache: "no-store",
  });
}

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
  appendIfPresent(
    params,
    "administrativeUnitName",
    filters.administrativeUnitName,
  );
  appendIfPresent(params, "category", filters.category);
  appendIfPresent(params, "indoor", filters.indoor);
  appendIfPresent(params, "maxCost", filters.maxCost);
  appendIfPresent(params, "page", filters.page);
  appendIfPresent(params, "size", filters.size);

  return requestPlaceJson<PlacePage>(`/api/v1/places?${params.toString()}`);
}

export async function getPlaceCatalog() {
  return requestPlaceJson<PlacePage>("/api/v1/places?page=0&size=100");
}

export async function getPlaceDetail(slug: string) {
  return requestPlaceJson<PlaceDetail>(
    `/api/v1/places/${encodeURIComponent(slug)}`,
  );
}
