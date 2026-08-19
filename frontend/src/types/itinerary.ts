export type ItineraryPlaceResponse = {
  slug: string;
  name: string;
  latitude: number;
  longitude: number;
  primaryImageUrl: string | null;
};

export type ItineraryScheduleResponse = {
  arrivalTime: string;
  visitStartTime: string;
  visitEndTime: string;
  travelMinutes: number;
  travelDistanceKm: number;
  estimatedCost: number;
};

export type ItineraryItemResponse = {
  publicId: string;
  sequenceNo: number;
  place: ItineraryPlaceResponse;
  schedule: ItineraryScheduleResponse;
};

export type ItinerarySummaryResponse = {
  totalEstimatedCost: number;
  totalTravelMinutes: number;
  totalVisitMinutes: number;
  totalDistanceKm: number;
};

export type ItineraryIssueType =
  | "PLACE_CLOSED"
  | "OPENING_HOURS_UNKNOWN"
  | "ENDS_AFTER_CLOSING"
  | "ENDS_AFTER_TRIP"
  | "OVER_BUDGET";

export type ItineraryIssueResponse = {
  type: ItineraryIssueType;
  placeSlug: string | null;
};

export type ItineraryResponse = {
  publicId: string | null;
  tripPublicId: string;
  items: ItineraryItemResponse[];
  summary: ItinerarySummaryResponse;
  issues: ItineraryIssueResponse[];
};

export type GenerateItineraryPreviewRequest = {
  preferenceDescription: string;
};

export type GeneratedItineraryStopResponse = {
  sequenceNo: number;
  place: ItineraryPlaceResponse;
  schedule: ItineraryScheduleResponse;
};

export type ItineraryGenerationPreviewResponse = {
  stops: GeneratedItineraryStopResponse[];
  summary: ItinerarySummaryResponse;
  issues: ItineraryIssueResponse[];
};

export type ApplyGeneratedItineraryRequest = {
  placeSlugs: string[];
};

export type SaveItineraryItemRequest = {
  placeId: number;
};

export type ReorderItineraryItemsRequest = {
  itemPublicIds: string[];
};
