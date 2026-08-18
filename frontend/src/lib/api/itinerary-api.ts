import { requestJson } from "@/lib/api/api-client";
import type {
  ApplyGeneratedItineraryRequest,
  GenerateItineraryPreviewRequest,
  ItineraryGenerationPreviewResponse,
  ItineraryResponse,
  ReorderItineraryItemsRequest,
  SaveItineraryItemRequest,
} from "@/types/itinerary";

const DEFAULT_BROWSER_BACKEND_URL = "http://localhost:8080";

function getBrowserBackendBaseUrl() {
  return (
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ??
    DEFAULT_BROWSER_BACKEND_URL
  ).replace(/\/$/, "");
}

function getItineraryUrl(tripPublicId: string, path = "") {
  return `${getBrowserBackendBaseUrl()}/api/v1/trips/${encodeURIComponent(tripPublicId)}/itinerary${path}`;
}

export function getItinerary(tripPublicId: string, token: string) {
  return requestJson<ItineraryResponse>(getItineraryUrl(tripPublicId), {
    token,
    cache: "no-store",
  });
}

export function addItineraryItem(
  tripPublicId: string,
  request: SaveItineraryItemRequest,
  token: string,
) {
  return requestJson<ItineraryResponse>(
    getItineraryUrl(tripPublicId, "/items"),
    {
      method: "POST",
      body: request,
      token,
      cache: "no-store",
    },
  );
}

export function deleteItineraryItem(
  tripPublicId: string,
  itemPublicId: string,
  token: string,
) {
  return requestJson<ItineraryResponse>(
    getItineraryUrl(
      tripPublicId,
      `/items/${encodeURIComponent(itemPublicId)}`,
    ),
    {
      method: "DELETE",
      token,
      cache: "no-store",
    },
  );
}

export function replaceItineraryItem(
  tripPublicId: string,
  itemPublicId: string,
  request: SaveItineraryItemRequest,
  token: string,
) {
  return requestJson<ItineraryResponse>(
    getItineraryUrl(
      tripPublicId,
      `/items/${encodeURIComponent(itemPublicId)}`,
    ),
    {
      method: "PUT",
      body: request,
      token,
      cache: "no-store",
    },
  );
}

export function reorderItineraryItems(
  tripPublicId: string,
  request: ReorderItineraryItemsRequest,
  token: string,
) {
  return requestJson<ItineraryResponse>(
    getItineraryUrl(tripPublicId, "/items/order"),
    {
      method: "PUT",
      body: request,
      token,
      cache: "no-store",
    },
  );
}

export function generateItineraryPreview(
  tripPublicId: string,
  request: GenerateItineraryPreviewRequest,
  token: string,
) {
  return requestJson<ItineraryGenerationPreviewResponse>(
    getItineraryUrl(tripPublicId, "/generation-preview"),
    {
      method: "POST",
      body: request,
      token,
      cache: "no-store",
    },
  );
}

export function applyGeneratedItinerary(
  tripPublicId: string,
  request: ApplyGeneratedItineraryRequest,
  token: string,
) {
  return requestJson<ItineraryResponse>(
    getItineraryUrl(tripPublicId, "/generation-apply"),
    {
      method: "POST",
      body: request,
      token,
      cache: "no-store",
    },
  );
}
