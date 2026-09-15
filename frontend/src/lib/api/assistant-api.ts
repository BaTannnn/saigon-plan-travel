import { requestJson } from "@/lib/api/api-client";
import type {
  AssistantMessageRequest,
  AssistantMessageResponse,
} from "@/types/assistant";

export function sendAssistantMessage(
  tripPublicId: string,
  request: AssistantMessageRequest,
  token: string,
) {
  return requestJson<AssistantMessageResponse>(
    `/api/v1/trips/${encodeURIComponent(tripPublicId)}/assistant/messages`,
    {
      method: "POST",
      body: request,
      token,
      cache: "no-store",
    },
  );
}
