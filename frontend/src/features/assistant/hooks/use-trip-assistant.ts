import { useRef, useState } from "react";
import type { RunAuthenticated } from "@/features/auth/auth-provider";
import { ApiError } from "@/lib/api/api-client";
import { sendAssistantMessage } from "@/lib/api/assistant-api";
import type {
  AssistantChatMessage,
  AssistantHistoryMessage,
} from "@/types/assistant";

const HISTORY_LIMIT = 10;

type UseTripAssistantOptions = {
  tripPublicId: string;
  runAuthenticated: RunAuthenticated;
};

type FailedRequest = {
  message: string;
  history: AssistantHistoryMessage[];
};

function createMessageId() {
  return globalThis.crypto.randomUUID();
}

function toHistory(
  messages: AssistantChatMessage[],
): AssistantHistoryMessage[] {
  return messages.slice(-HISTORY_LIMIT).map((message) => ({
    role: message.role === "user" ? "USER" : "ASSISTANT",
    content: message.content,
  }));
}

function getAssistantErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 0) {
      return "Không thể kết nối đến trợ lý. Vui lòng thử lại.";
    }
    return error.problem?.detail ?? "Trợ lý chưa thể trả lời lúc này.";
  }

  return "Đã xảy ra lỗi khi gửi câu hỏi.";
}

export function useTripAssistant({
  tripPublicId,
  runAuthenticated,
}: UseTripAssistantOptions) {
  const [messages, setMessages] = useState<AssistantChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [failedRequest, setFailedRequest] = useState<FailedRequest | null>(
    null,
  );
  const sendingRef = useRef(false);

  async function requestAnswer(
    message: string,
    history: AssistantHistoryMessage[],
  ) {
    if (sendingRef.current) return;

    sendingRef.current = true;
    setIsSending(true);
    setError(null);

    try {
      const response = await runAuthenticated((token) =>
        sendAssistantMessage(tripPublicId, { message, history }, token),
      );
      setMessages((current) => [
        ...current,
        {
          id: createMessageId(),
          role: "assistant",
          content: response.answer,
          suggestedPlaces: response.suggestedPlaces,
          sources: response.sources,
        },
      ]);
      setFailedRequest(null);
    } catch (requestError) {
      setError(getAssistantErrorMessage(requestError));
      setFailedRequest({ message, history });
    } finally {
      sendingRef.current = false;
      setIsSending(false);
    }
  }

  async function sendMessage(value = input) {
    const message = value.trim();
    if (message.length < 3 || sendingRef.current) return;

    const history = toHistory(messages);
    setMessages((current) => [
      ...current,
      { id: createMessageId(), role: "user", content: message },
    ]);
    setInput("");
    setFailedRequest(null);
    await requestAnswer(message, history);
  }

  async function retry() {
    if (!failedRequest || sendingRef.current) return;
    await requestAnswer(failedRequest.message, failedRequest.history);
  }

  return {
    messages,
    input,
    setInput,
    isSending,
    error,
    sendMessage,
    retry,
  };
}
