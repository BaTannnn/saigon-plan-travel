import { ArrowRight, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
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

type ItineraryWarningSummaryProps = {
  issues: ItineraryIssueResponse[];
  places: ItineraryPlaceResponse[];
};

type IssueGroup = {
  key: string;
  heading: string;
  issues: ItineraryIssueResponse[];
};

const issueLabels: Record<ItineraryIssueType, string> = {
  PLACE_CLOSED: "Địa điểm đóng cửa",
  OPENING_HOURS_UNKNOWN: "Chưa có thông tin giờ mở cửa",
  ENDS_AFTER_CLOSING: "Lịch tham quan kết thúc sau giờ đóng cửa",
  ENDS_AFTER_TRIP: "Lịch trình vượt quá thời gian chuyến đi",
  OVER_BUDGET: "Lịch trình vượt ngân sách",
};

function groupIssuesByPlace(
  issues: ItineraryIssueResponse[],
  places: ItineraryPlaceResponse[],
) {
  const placeNames = new Map(places.map((place) => [place.slug, place.name]));
  const groups = new Map<string, IssueGroup>();

  issues.forEach((issue) => {
    const placeName = issue.placeSlug
      ? placeNames.get(issue.placeSlug)
      : undefined;
    const key =
      placeName && issue.placeSlug ? `place:${issue.placeSlug}` : "general";
    const heading = placeName ?? "Cảnh báo chung";
    const existingGroup = groups.get(key);

    if (existingGroup) {
      existingGroup.issues.push(issue);
      return;
    }

    groups.set(key, { key, heading, issues: [issue] });
  });

  return Array.from(groups.values());
}

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

export function ItineraryWarningSummary({
  issues,
  places,
}: ItineraryWarningSummaryProps) {
  if (issues.length === 0) return null;

  const groups = groupIssuesByPlace(issues, places);

  return (
    <Dialog>
      <section
        className="mt-5 flex flex-col gap-3 border-y border-ochre/30 bg-ochre-soft/75 px-3 py-3 text-ochre-foreground sm:flex-row sm:items-center sm:justify-between sm:px-4"
        aria-labelledby="itinerary-warning-summary-heading"
      >
        <div className="flex min-w-0 items-start gap-3">
          <TriangleAlert
            className="mt-0.5 size-5 shrink-0 text-ochre"
            aria-hidden="true"
          />
          <div className="min-w-0">
            <h2
              id="itinerary-warning-summary-heading"
              className="m-0 text-sm font-bold"
            >
              Lịch trình có {issues.length} cảnh báo
            </h2>
            <p className="mt-0.5 mb-0 text-xs leading-5 text-ochre-foreground/80">
              Một số địa điểm cần được kiểm tra.
            </p>
          </div>
        </div>

        <DialogTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="self-end text-ochre-foreground hover:bg-ochre/10 hover:text-ochre-foreground sm:self-auto"
          >
            Xem chi tiết
            <ArrowRight className="size-4" aria-hidden="true" />
          </Button>
        </DialogTrigger>
      </section>

      <DialogContent className="w-[min(calc(100vw_-_2rem),520px)]">
        <DialogHeader className="border-b border-border pb-5">
          <DialogTitle>Cảnh báo lịch trình</DialogTitle>
          <DialogDescription>
            {issues.length} vấn đề được phát hiện
          </DialogDescription>
        </DialogHeader>

        <div className="max-h-[60dvh] overflow-y-auto px-5 py-5">
          <div className="grid gap-5">
            {groups.map((group, groupIndex) => (
              <section
                key={group.key}
                aria-labelledby={`itinerary-warning-group-${groupIndex}`}
              >
                <h3
                  id={`itinerary-warning-group-${groupIndex}`}
                  className="m-0 text-sm font-bold text-text-primary"
                >
                  {group.heading}
                </h3>
                <ul className="mt-2 grid gap-2">
                  {group.issues.map((issue, index) => (
                    <li
                      key={`${issue.type}-${issue.placeSlug ?? "trip"}-${index}`}
                      className="flex items-start gap-2 text-sm leading-5 text-ochre-foreground"
                    >
                      <TriangleAlert
                        className="mt-0.5 size-4 shrink-0 text-ochre"
                        aria-hidden="true"
                      />
                      <span>{issueLabels[issue.type]}</span>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
