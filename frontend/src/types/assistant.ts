export type AssistantHistoryMessage = {
  role: "USER" | "ASSISTANT";
  content: string;
};

export type AssistantMessageRequest = {
  message: string;
  history: AssistantHistoryMessage[];
};

export type AssistantSuggestedPlace = {
  placeId: number;
  slug: string;
  name: string;
  primaryImageUrl: string | null;
  reason: string;
};

export type PlaceAssistantSource = {
  type: "PLACE";
  placeSlug: string;
  placeName: string;
  section: string;
};

export type DocumentAssistantSource = {
  type: "DOCUMENT";
  title: string;
  pageNumber: number;
  sourceLabel: string | null;
};

export type AssistantSource =
  | PlaceAssistantSource
  | DocumentAssistantSource;

export type AssistantMessageResponse = {
  answer: string;
  suggestedPlaces: AssistantSuggestedPlace[];
  sources: AssistantSource[];
};

export type AssistantChatMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  suggestedPlaces?: AssistantSuggestedPlace[];
  sources?: AssistantSource[];
};
