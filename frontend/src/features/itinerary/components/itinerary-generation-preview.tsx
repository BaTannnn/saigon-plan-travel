import { Clock3, Route, WalletCards } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  ItineraryIssues,
  ItinerarySummary,
} from "@/features/itinerary/components/itinerary-insights";
import {
  formatCurrency,
  formatDistance,
  formatTime,
} from "@/lib/formatters";
import type { ItineraryGenerationPreviewResponse } from "@/types/itinerary";

type ItineraryGenerationPreviewProps = {
  preview: ItineraryGenerationPreviewResponse;
  applyError: string | null;
  applyLoading: boolean;
  generationLoading: boolean;
  onApply: () => Promise<void>;
};

export function ItineraryGenerationPreview({
  preview,
  applyError,
  applyLoading,
  generationLoading,
  onApply,
}: ItineraryGenerationPreviewProps) {
  return (
    <section className="mt-7 border-t border-border pt-6">
      <h2 className="m-0 text-lg font-bold text-text-primary">
        Gợi ý hành trình
      </h2>
      <p className="mt-1 mb-0 text-sm text-text-secondary">
        Đây là bản xem trước và chưa thay đổi hành trình hiện tại.
      </p>

      {preview.stops.length === 0 ? (
        <div className="mt-4 rounded-xl border border-dashed border-border bg-surface/60 px-4 py-8 text-center text-sm text-text-secondary">
          Chưa tìm được lịch trình phù hợp với các điều kiện hiện tại.
        </div>
      ) : (
        <ol className="mt-4 grid gap-3">
          {preview.stops.map((stop, index) => (
            <li
              key={`${stop.sequenceNo}-${stop.place.slug}`}
              className={`relative grid grid-cols-[40px_minmax(0,1fr)] gap-3 ${
                index < preview.stops.length - 1
                  ? "after:absolute after:top-10 after:bottom-[-12px] after:left-5 after:w-px after:bg-primary/25 after:content-['']"
                  : ""
              }`}
            >
              <span className="z-10 grid size-9 place-items-center rounded-full bg-primary text-xs font-black text-primary-foreground">
                {String(stop.sequenceNo).padStart(2, "0")}
              </span>
              <div className="min-w-0 border-b border-border pb-3">
                <strong className="block truncate text-[0.95rem] text-text-primary">
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
                    {formatDistance(stop.schedule.travelDistanceKm)}
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
        summary={preview.summary}
        headingId="generation-preview-summary-heading"
      />
      <ItineraryIssues
        issues={preview.issues}
        places={preview.stops.map((stop) => stop.place)}
        headingId="generation-preview-issues-heading"
      />

      {preview.stops.length > 0 ? (
        <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-5">
          {applyError ? (
            <p
              className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-sm font-semibold text-destructive"
              role="alert"
            >
              {applyError}
            </p>
          ) : null}
          <Button
            type="button"
            variant="accent"
            onClick={onApply}
            disabled={applyLoading || generationLoading}
          >
            {applyLoading ? "Đang áp dụng..." : "Áp dụng hành trình"}
          </Button>
        </div>
      ) : null}
    </section>
  );
}
