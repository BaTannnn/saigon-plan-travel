import { requestJson } from "@/lib/api/api-client";
import type {
  SaveTripRequest,
  TripResponse,
  TripSummaryResponse,
} from "@/types/trip";

const DEFAULT_BROWSER_BACKEND_URL = "http://localhost:8080";

function getBrowserBackendBaseUrl() {
  return (
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ??
    DEFAULT_BROWSER_BACKEND_URL
  ).replace(/\/$/, "");
}

function getTripUrl(path = "") {
  return `${getBrowserBackendBaseUrl()}/api/v1/trips${path}`;
}

export function createTrip(request: SaveTripRequest, token: string) {
  return requestJson<TripResponse>(getTripUrl(), {
    method: "POST",
    body: request,
    token,
    cache: "no-store",
  });
}

export function getTrips(token: string) {
  return requestJson<TripSummaryResponse[]>(getTripUrl(), {
    token,
    cache: "no-store",
  });
}

export function getTrip(publicId: string, token: string) {
  return requestJson<TripResponse>(
    getTripUrl(`/${encodeURIComponent(publicId)}`),
    {
      token,
      cache: "no-store",
    },
  );
}

export function replaceTrip(
  publicId: string,
  request: SaveTripRequest,
  token: string,
) {
  return requestJson<TripResponse>(
    getTripUrl(`/${encodeURIComponent(publicId)}`),
    {
      method: "PUT",
      body: request,
      token,
      cache: "no-store",
    },
  );
}

export function deleteTrip(publicId: string, token: string) {
  return requestJson<void>(getTripUrl(`/${encodeURIComponent(publicId)}`), {
    method: "DELETE",
    token,
    cache: "no-store",
  });
}
