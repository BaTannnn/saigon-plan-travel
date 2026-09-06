import { requestJson } from "@/lib/api/api-client";
import type {
  SaveTripRequest,
  TripResponse,
  TripSummaryResponse,
} from "@/types/trip";

function getTripPath(path = "") {
  return `/api/v1/trips${path}`;
}

export type TripListFilter = {
  year: number;
  month: number;
};

export function createTrip(request: SaveTripRequest, token: string) {
  return requestJson<TripResponse>(getTripPath(), {
    method: "POST",
    body: request,
    token,
    cache: "no-store",
  });
}

export function getTrips(filter: TripListFilter, token: string) {
  const searchParams = new URLSearchParams({
    year: String(filter.year),
    month: String(filter.month),
  });

  return requestJson<TripSummaryResponse[]>(
    getTripPath(`?${searchParams}`),
    {
      method: "GET",
      token,
      cache: "no-store",
    },
  );
}

export function getTrip(publicId: string, token: string) {
  return requestJson<TripResponse>(
    getTripPath(`/${encodeURIComponent(publicId)}`),
    {
      method: "GET",
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
    getTripPath(`/${encodeURIComponent(publicId)}`),
    {
      method: "PUT",
      body: request,
      token,
      cache: "no-store",
    },
  );
}

export function deleteTrip(publicId: string, token: string) {
  return requestJson<void>(getTripPath(`/${encodeURIComponent(publicId)}`), {
    method: "DELETE",
    token,
    cache: "no-store",
  });
}
