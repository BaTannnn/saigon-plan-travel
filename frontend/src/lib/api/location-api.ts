import { requestJson } from "@/lib/api/api-client";
import type { LocationSearchResult } from "@/types/location";

const DEFAULT_BROWSER_BACKEND_URL = "http://localhost:8080";

function getBrowserBackendBaseUrl() {
  return (
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ??
    DEFAULT_BROWSER_BACKEND_URL
  ).replace(/\/$/, "");
}

export function searchLocations(query: string, token: string) {
  const params = new URLSearchParams({ q: query.trim() });

  return requestJson<LocationSearchResult[]>(
    `${getBrowserBackendBaseUrl()}/api/v1/locations/search?${params.toString()}`,
    {
      token,
      cache: "no-store",
    },
  );
}
