import { TriangleAlert } from "lucide-react";
import {
  formatCurrency,
  formatDistance,
  formatDuration,
} from "@/lib/formatters";
import type {
  ItineraryIssueResponse,
  ItineraryIssueType,
  ItineraryPlaceResponse,
  ItinerarySummaryResponse,
} from "@/types/itinerary";

type ItinerarySummaryProps = {
  summary: ItinerarySummaryResponse;
  headingId: string;
};

type ItineraryIssuesProps = {
  issues: ItineraryIssueResponse[];
  places: ItineraryPlaceResponse[];
  headingId: string;
};

const issueLabels: Record<ItineraryIssueType, string> = {
  PLACE_CLOSED: "Địa điểm đóng cửa",
  OPENING_HOURS_UNKNOWN: "Chưa có thông tin giờ mở cửa",
  ENDS_AFTER_CLOSING: "Lịch tham quan kết thúc sau giờ đóng cửa",
  ENDS_AFTER_TRIP: "Lịch trình vượt quá thời gian chuyến đi",
  OVER_BUDGET: "Lịch trình vượt ngân sách",
};

export function ItinerarySummary({
  summary,
  headingId,
}: ItinerarySummaryProps) {
  return (
    <section className="mt-6" aria-labelledby={headingId}>
      <h2
        id={headingId}
        className="m-0 text-xs font-extrabold tracking-[0.12em] text-text-secondary uppercase"
      >
        Tổng quan lịch trình
      </h2>
      <dl className="mt-3 grid grid-cols-2 border-y border-border sm:grid-cols-4">
        <div className="py-2.5 pr-3 sm:px-3 sm:first:pl-0">
          <dt className="text-xs text-text-secondary">Chi phí</dt>
          <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
            {formatCurrency(summary.totalEstimatedCost)}
          </dd>
        </div>
        <div className="py-2.5 pl-3 sm:border-l sm:px-3">
          <dt className="text-xs text-text-secondary">Di chuyển</dt>
          <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
            {formatDuration(summary.totalTravelMinutes)}
          </dd>
        </div>
        <div className="py-2.5 pr-3 sm:border-l sm:px-3">
          <dt className="text-xs text-text-secondary">Tham quan</dt>
          <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
            {formatDuration(summary.totalVisitMinutes)}
          </dd>
        </div>
        <div className="py-2.5 pl-3 sm:border-l sm:pl-3 sm:pr-0">
          <dt className="text-xs text-text-secondary">Quãng đường</dt>
          <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
            {formatDistance(summary.totalDistanceKm)}
          </dd>
        </div>
      </dl>
    </section>
  );
}

export function ItineraryIssues({
  issues,
  places,
  headingId,
}: ItineraryIssuesProps) {
  if (issues.length === 0) return null;

  return (
    <section className="mt-5" aria-labelledby={headingId}>
      <h2 id={headingId} className="sr-only">
        Lưu ý lịch trình
      </h2>
      <ul className="divide-y divide-ochre/20 border-y border-ochre/30 bg-ochre-soft/75">
        {issues.map((issue, index) => {
          const placeName = issue.placeSlug
            ? places.find((place) => place.slug === issue.placeSlug)?.name
            : null;

          return (
            <li
              key={`${issue.type}-${issue.placeSlug ?? "trip"}-${index}`}
              className="flex items-start gap-2 px-3 py-2 text-sm text-ochre-foreground"
            >
              <TriangleAlert
                className="mt-0.5 size-4 shrink-0 text-ochre"
                aria-hidden="true"
              />
              <span>
                <strong>{issueLabels[issue.type]}</strong>
                {placeName ? ` · ${placeName}` : ""}
              </span>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
