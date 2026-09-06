import { requestJson } from "@/lib/api/api-client";
import type {
  ApplyGeneratedItineraryRequest,
  GenerateItineraryPreviewRequest,
  ItineraryGenerationPreviewResponse,
  ItineraryResponse,
  ReorderItineraryItemsRequest,
  SaveItineraryItemRequest,
} from "@/types/itinerary";

function getItineraryPath(tripPublicId: string, path = "") {
  return `/api/v1/trips/${encodeURIComponent(tripPublicId)}/itinerary${path}`;
}

export function getItinerary(tripPublicId: string, token: string) {
  return requestJson<ItineraryResponse>(getItineraryPath(tripPublicId), {
    method: "GET",
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
    getItineraryPath(tripPublicId, "/items"),
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
    getItineraryPath(
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
    getItineraryPath(
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
    getItineraryPath(tripPublicId, "/items/order"),
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
    getItineraryPath(tripPublicId, "/generation-preview"),
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
    getItineraryPath(tripPublicId, "/generation-apply"),
    {
      method: "POST",
      body: request,
      token,
      cache: "no-store",
    },
  );
}
