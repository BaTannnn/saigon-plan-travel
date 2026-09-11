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

export type AssistantMessageResponse = {
  answer: string;
  suggestedPlaces: AssistantSuggestedPlace[];
};

export type AssistantChatMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  suggestedPlaces?: AssistantSuggestedPlace[];
};
