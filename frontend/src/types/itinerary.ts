import type { PlaceSummary } from "@/types/place";

export type ItineraryItemResponse = {
  publicId: string;
  sequenceNo: number;
  place: PlaceSummary;
  createdAt: string;
  updatedAt: string;
};

export type ItineraryResponse = {
  publicId: string | null;
  tripPublicId: string;
  items: ItineraryItemResponse[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type SaveItineraryItemRequest = {
  placeId: number;
};
