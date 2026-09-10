"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { Bot, FileText, LoaderCircle, MapPin, Send, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { SuggestedPlaceCard } from "@/features/assistant/components/suggested-place-card";
import { useTripAssistant } from "@/features/assistant/hooks/use-trip-assistant";
import { useAuth } from "@/features/auth/auth-provider";
import type { AssistantChatMessage, AssistantSource } from "@/types/assistant";

const STARTER_PROMPTS = [
  "Gợi ý cho tôi địa điểm lịch sử",
  "Có bảo tàng nào đáng tham quan?",
  "Tôi thích nghệ thuật, nên đi đâu?",
];

type TripAssistantProps = {
  tripPublicId: string;
  itineraryPlaceSlugs: string[];
  onAddPlace: (placeId: number) => Promise<void>;
};

function AssistantSources({ sources }: { sources: AssistantSource[] }) {
  if (sources.length === 0) return null;

  return (
    <section className="mt-3 border-t border-border pt-3">
      <h4 className="m-0 text-[0.68rem] font-extrabold tracking-[0.1em] text-text-secondary uppercase">
        Tài liệu tham khảo
      </h4>
      <ul className="mt-2 grid list-none gap-1.5 p-0 text-xs text-text-secondary">
        {sources.map((source, index) => (
          <li
            key={`${source.type}-${index}`}
            className="flex min-w-0 items-start gap-1.5"
          >
            {source.type === "PLACE" ? (
              <MapPin className="mt-0.5 size-3.5 shrink-0 text-primary" aria-hidden="true" />
            ) : (
              <FileText className="mt-0.5 size-3.5 shrink-0 text-ochre" aria-hidden="true" />
            )}
            <span className="min-w-0 wrap-break-word">
              {source.type === "PLACE"
                ? `${source.placeName} · ${source.section}`
                : `${source.title} · Trang ${source.pageNumber}`}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function ChatMessage({
  message,
  itineraryPlaceSlugs,
  onAddPlace,
}: {
  message: AssistantChatMessage;
  itineraryPlaceSlugs: Set<string>;
  onAddPlace: (placeId: number) => Promise<void>;
}) {
  const isUser = message.role === "user";

  return (
    <div className={isUser ? "flex justify-end" : "flex justify-start"}>
      <div
        className={
          isUser
            ? "max-w-[85%] rounded-2xl rounded-br-sm bg-primary px-3.5 py-2.5 text-sm leading-6 text-primary-foreground"
            : "w-full max-w-[94%] rounded-2xl rounded-bl-sm border border-border bg-surface px-3.5 py-3 text-sm leading-6 text-text-primary"
        }
      >
        <p className="m-0 whitespace-pre-wrap wrap-break-word">{message.content}</p>

        {!isUser && message.suggestedPlaces?.length ? (
          <div className="mt-3 grid gap-2.5">
            {message.suggestedPlaces.map((place) => (
              <SuggestedPlaceCard
                key={`${place.slug}-${itineraryPlaceSlugs.has(place.slug)}`}
                place={place}
                alreadyInItinerary={itineraryPlaceSlugs.has(place.slug)}
                onAddPlace={onAddPlace}
              />
            ))}
          </div>
        ) : null}

        {!isUser && message.sources ? (
          <AssistantSources sources={message.sources} />
        ) : null}
      </div>
    </div>
  );
}

export function TripAssistant({
  tripPublicId,
  itineraryPlaceSlugs,
  onAddPlace,
}: TripAssistantProps) {
  const { runAuthenticated } = useAuth();
  const assistant = useTripAssistant({ tripPublicId, runAuthenticated });
  const [open, setOpen] = useState(false);
  const endRef = useRef<HTMLDivElement | null>(null);
  const currentPlaceSlugs = new Set(itineraryPlaceSlugs);

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ block: "nearest" });
  }, [assistant.error, assistant.isSending, assistant.messages, open]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void assistant.sendMessage();
  }

  function handleInputKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (
      event.key === "Enter" &&
      !event.shiftKey &&
      !event.nativeEvent.isComposing
    ) {
      event.preventDefault();
      void assistant.sendMessage();
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          type="button"
          variant="accent"
          className="fixed right-5 bottom-5 z-[900] shadow-mint-md sm:right-7 sm:bottom-7"
          aria-label="Mở trợ lý du lịch AI"
        >
          <Sparkles className="size-4" aria-hidden="true" />
          Trợ lý AI
        </Button>
      </DialogTrigger>

      <DialogContent className="top-0 right-0 left-auto h-dvh max-h-dvh w-[min(100vw,410px)] translate-x-0 translate-y-0 rounded-none border-y-0 border-r-0 shadow-mint-md md:top-20 md:h-[calc(100dvh_-_5rem)] md:max-h-[calc(100dvh_-_5rem)]">
        <DialogHeader className="border-b border-border pb-4">
          <DialogTitle className="flex items-center gap-2">
            <span className="grid size-8 place-items-center rounded-full bg-accent-soft text-accent-strong">
              <Sparkles className="size-4" aria-hidden="true" />
            </span>
            SaigonPlan AI
          </DialogTitle>
          <DialogDescription>
            Hỏi về địa điểm và nhận gợi ý để tự thêm vào hành trình.
          </DialogDescription>
        </DialogHeader>

        <div className="min-h-0 flex-1 overflow-y-auto bg-muted/25 px-4 py-4">
          {assistant.messages.length === 0 ? (
            <div className="grid min-h-full content-center gap-5 py-8 text-center">
              <div>
                <span className="mx-auto grid size-11 place-items-center rounded-full bg-primary-soft text-primary">
                  <Bot className="size-5" aria-hidden="true" />
                </span>
                <h3 className="mt-3 mb-0 text-base font-bold">Bạn muốn khám phá gì?</h3>
                <p className="mx-auto mt-1.5 mb-0 max-w-xs text-sm leading-6 text-text-secondary">
                  Tôi có thể tìm trong dữ liệu địa điểm và cẩm nang du lịch của SaigonPlanTravel.
                </p>
              </div>
              <div className="grid gap-2">
                {STARTER_PROMPTS.map((prompt) => (
                  <Button
                    key={prompt}
                    type="button"
                    size="sm"
                    variant="outline"
                    className="h-auto justify-start py-2.5 text-left whitespace-normal"
                    disabled={assistant.isSending}
                    onClick={() => void assistant.sendMessage(prompt)}
                  >
                    {prompt}
                  </Button>
                ))}
              </div>
            </div>
          ) : (
            <div className="grid gap-3" aria-live="polite">
              {assistant.messages.map((message) => (
                <ChatMessage
                  key={message.id}
                  message={message}
                  itineraryPlaceSlugs={currentPlaceSlugs}
                  onAddPlace={onAddPlace}
                />
              ))}

              {assistant.isSending ? (
                <div className="flex items-center gap-2 text-sm text-text-secondary">
                  <LoaderCircle className="size-4 animate-spin text-primary" aria-hidden="true" />
                  AI đang trả lời...
                </div>
              ) : null}

              {assistant.error ? (
                <div className="border-l-2 border-destructive bg-destructive/8 px-3 py-2.5" role="alert">
                  <p className="m-0 text-sm text-destructive">{assistant.error}</p>
                  <Button
                    type="button"
                    size="xs"
                    variant="outline"
                    className="mt-2"
                    disabled={assistant.isSending}
                    onClick={() => void assistant.retry()}
                  >
                    Thử gửi lại
                  </Button>
                </div>
              ) : null}
            </div>
          )}
          <div ref={endRef} />
        </div>

        <form className="border-t border-border bg-surface p-3" onSubmit={handleSubmit}>
          <div className="flex items-end gap-2 rounded-mint-md border border-input bg-background p-2 focus-within:ring-3 focus-within:ring-ring/25">
            <textarea
              value={assistant.input}
              onChange={(event) => assistant.setInput(event.target.value)}
              onKeyDown={handleInputKeyDown}
              rows={2}
              maxLength={1000}
              placeholder="Nhập câu hỏi..."
              aria-label="Câu hỏi cho trợ lý AI"
              className="min-h-10 flex-1 resize-none border-0 bg-transparent px-1 py-1.5 text-sm leading-5 outline-none placeholder:text-muted-foreground"
            />
            <Button
              type="submit"
              size="icon-sm"
              disabled={assistant.input.trim().length < 3 || assistant.isSending}
              aria-label="Gửi câu hỏi"
            >
              {assistant.isSending ? (
                <LoaderCircle className="animate-spin" aria-hidden="true" />
              ) : (
                <Send aria-hidden="true" />
              )}
            </Button>
          </div>
          <p className="mt-1.5 mb-0 px-1 text-[0.68rem] text-text-secondary">
            Enter để gửi · Shift + Enter để xuống dòng
          </p>
        </form>
      </DialogContent>
    </Dialog>
  );
}
