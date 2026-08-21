import { requestJson } from "@/lib/api/api-client";
import type { LocationSearchResult } from "@/types/location";

export function searchLocations(query: string, token: string) {
  const params = new URLSearchParams({ q: query.trim() });

  return requestJson<LocationSearchResult[]>(
    `/api/v1/locations/search?${params.toString()}`,
    {
      method: "GET",
      token,
      cache: "no-store",
    },
  );
}
