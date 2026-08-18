"use client";

import { FormEvent, useState } from "react";
import { Clock3, Route, Sparkles, WalletCards } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import {
  ItineraryIssues,
  ItinerarySummary,
} from "@/features/itinerary/components/itinerary-insights";
import { ApiError } from "@/lib/api/api-client";
import {
  formatCurrency,
  formatDistance,
  formatTime,
} from "@/lib/formatters";
import type { ItineraryGenerationPreviewResponse } from "@/types/itinerary";

type ItineraryGenerationSheetProps = {
  onGenerate: (
    preferenceDescription: string,
  ) => Promise<ItineraryGenerationPreviewResponse>;
};

function getGenerationErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 0) {
      return "Không thể kết nối đến máy chủ.";
    }

    return error.problem?.detail ?? "Không thể tạo gợi ý hành trình.";
  }

  return "Đã xảy ra lỗi khi tạo gợi ý hành trình.";
}

export function ItineraryGenerationSheet({
  onGenerate,
}: ItineraryGenerationSheetProps) {
  const [open, setOpen] = useState(false);
  const [preferenceDescription, setPreferenceDescription] = useState("");
  const [generationPreview, setGenerationPreview] =
    useState<ItineraryGenerationPreviewResponse | null>(null);
  const [generationLoading, setGenerationLoading] = useState(false);
  const [generationError, setGenerationError] = useState<string | null>(null);

  const preference = preferenceDescription.trim();

  async function handleGenerate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!preference || generationLoading) return;

    setGenerationLoading(true);
    setGenerationError(null);
    setGenerationPreview(null);

    try {
      const response = await onGenerate(preference);
      setGenerationPreview(response);
    } catch (error) {
      setGenerationError(getGenerationErrorMessage(error));
    } finally {
      setGenerationLoading(false);
    }
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button type="button" variant="outline" size="sm">
          <Sparkles className="size-4 text-primary" aria-hidden="true" />
          Tạo hành trình tự động
        </Button>
      </SheetTrigger>

      <SheetContent className="w-[min(96vw,640px)] sm:max-w-[640px]">
        <SheetHeader className="border-b border-border px-5 py-5 pr-14">
          <SheetTitle className="text-lg font-bold">
            Tạo hành trình tự động
          </SheetTitle>
          <SheetDescription>
            Hệ thống sẽ đề xuất một lịch trình dựa trên sở thích của bạn và các
            điều kiện hiện có của chuyến đi.
          </SheetDescription>
        </SheetHeader>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5">
          <form className="grid gap-3" onSubmit={handleGenerate}>
            <div className="grid gap-1.5">
              <Label htmlFor="itinerary-generation-preference">
                Sở thích cho chuyến đi
              </Label>
              <textarea
                id="itinerary-generation-preference"
                value={preferenceDescription}
                onChange={(event) =>
                  setPreferenceDescription(event.target.value)
                }
                placeholder="Tôi thích lịch sử, kiến trúc, bảo tàng và muốn lịch trình nhẹ nhàng..."
                rows={4}
                disabled={generationLoading}
                className="min-h-28 w-full resize-y rounded-lg border border-input bg-surface px-3 py-2 text-sm text-text-primary outline-none transition placeholder:text-text-secondary/70 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/25 disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                type="submit"
                variant="accent"
                disabled={!preference || generationLoading}
              >
                <Sparkles className="size-4" aria-hidden="true" />
                {generationLoading ? "Đang tạo hành trình..." : "Tạo gợi ý"}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => setOpen(false)}
              >
                Hủy
              </Button>
            </div>
          </form>

          {generationError ? (
            <p
              className="mt-4 rounded-lg bg-destructive/10 px-3 py-2 text-sm font-semibold text-destructive"
              role="alert"
            >
              {generationError}
            </p>
          ) : null}

          {generationPreview ? (
            <section className="mt-7 border-t border-border pt-6">
              <h2 className="m-0 text-base font-bold text-text-primary">
                Gợi ý hành trình
              </h2>
              <p className="mt-1 mb-0 text-sm text-text-secondary">
                Đây là bản xem trước và chưa thay đổi hành trình hiện tại.
              </p>

              {generationPreview.stops.length === 0 ? (
                <div className="mt-4 rounded-xl border border-dashed border-border bg-surface/60 px-4 py-8 text-center text-sm text-text-secondary">
                  Chưa tìm được lịch trình phù hợp với các điều kiện hiện tại.
                </div>
              ) : (
                <ol className="mt-4 grid gap-3">
                  {generationPreview.stops.map((stop, index) => (
                    <li
                      key={`${stop.sequenceNo}-${stop.place.slug}`}
                      className={`relative grid grid-cols-[40px_minmax(0,1fr)] gap-3 ${
                        index < generationPreview.stops.length - 1
                          ? "after:absolute after:top-10 after:bottom-[-12px] after:left-5 after:w-px after:bg-primary/25 after:content-['']"
                          : ""
                      }`}
                    >
                      <span className="z-10 grid size-10 place-items-center rounded-full bg-primary text-sm font-black text-primary-foreground">
                        {stop.sequenceNo}
                      </span>
                      <div className="min-w-0 rounded-xl border border-border bg-surface p-3">
                        <strong className="block truncate text-base text-text-primary">
                          {stop.place.name}
                        </strong>
                        <span className="mt-1.5 flex items-center gap-1 text-[0.8rem] leading-5 font-semibold text-text-primary tabular-nums">
                          <Clock3
                            className="size-3.5 text-primary"
                            aria-hidden="true"
                          />
                          {formatTime(stop.schedule.visitStartTime)} –{" "}
                          {formatTime(stop.schedule.visitEndTime)}
                        </span>
                        <span className="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-xs leading-5 text-text-secondary">
                          <span className="inline-flex items-center gap-1 tabular-nums">
                            <Route className="size-3.5" aria-hidden="true" />
                            {stop.schedule.travelMinutes} phút ·{" "}
                            {formatDistance(
                              stop.schedule.travelDistanceKm,
                            )}
                          </span>
                          <span className="inline-flex items-center gap-1 text-ochre-foreground tabular-nums">
                            <WalletCards
                              className="size-3.5 text-ochre"
                              aria-hidden="true"
                            />
                            {formatCurrency(stop.schedule.estimatedCost)}
                          </span>
                        </span>
                      </div>
                    </li>
                  ))}
                </ol>
              )}

              <ItinerarySummary
                summary={generationPreview.summary}
                headingId="generation-preview-summary-heading"
              />
              <ItineraryIssues
                issues={generationPreview.issues}
                places={generationPreview.stops.map((stop) => stop.place)}
                headingId="generation-preview-issues-heading"
              />
            </section>
          ) : null}
        </div>
      </SheetContent>
    </Sheet>
  );
}
