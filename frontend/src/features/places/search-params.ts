import type { PlacesSearchFilters } from "@/types/place";

export type RawSearchParams = Record<string, string | string[] | undefined>;

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function optional(value: string | string[] | undefined) {
  const normalized = first(value)?.trim();
  return normalized ? normalized : undefined;
}

function integerOrDefault(
  value: string | string[] | undefined,
  fallback: number,
) {
  const raw = first(value);
  if (raw === undefined || !/^\d+$/.test(raw)) {
    return fallback;
  }

  return Number(raw);
}

export function readPlacesSearchParams(
  params: RawSearchParams,
): PlacesSearchFilters {
  const indoor = optional(params.indoor);

  return {
    keyword: optional(params.keyword),
    district: optional(params.district),
    category: optional(params.category),
    indoor: indoor === "true" || indoor === "false" ? indoor : undefined,
    maxCost: optional(params.maxCost),
    page: integerOrDefault(params.page, 0),
    size: integerOrDefault(params.size, 10),
  };
}

export function createPlacesHref(
  filters: PlacesSearchFilters,
  changes: Partial<PlacesSearchFilters>,
) {
  const next = { ...filters, ...changes };
  const params = new URLSearchParams();

  for (const [key, value] of Object.entries(next)) {
    if (value !== undefined && value !== "" && key !== "size") {
      params.set(key, String(value));
    }
  }

  const query = params.toString();
  return query ? `/places?${query}` : "/places";
}

export function hasActiveFilters(filters: PlacesSearchFilters) {
  return Boolean(
    filters.keyword ||
      filters.district ||
      filters.category ||
      filters.indoor ||
      filters.maxCost,
  );
}
