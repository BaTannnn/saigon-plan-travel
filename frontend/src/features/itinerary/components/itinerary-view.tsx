"use client";

import { useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  Clock3,
  MapPin,
  MoreHorizontal,
  PencilLine,
  Plus,
  Route,
  Trash2,
  TriangleAlert,
  WalletCards,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  PlacePickerSheet,
  type PlacePickerMode,
} from "@/features/itinerary/components/place-picker-sheet";
import { formatCurrency, formatDuration } from "@/lib/formatters";
import type {
  ItineraryIssueType,
  ItineraryResponse,
} from "@/types/itinerary";
import type { PlaceSummary } from "@/types/place";
import type { TripResponse } from "@/types/trip";

type PickerState = {
  mode: PlacePickerMode;
  itemPublicId: string | null;
};

type ItineraryViewProps = {
  trip: TripResponse;
  itinerary: ItineraryResponse | null;
  loading: boolean;
  error: string | null;
  selectedItemPublicId: string | null;
  mutating: boolean;
  onRetry: () => void;
  onAdd: (place: PlaceSummary) => Promise<void>;
  onDelete: (itemPublicId: string) => Promise<void>;
  onReplace: (itemPublicId: string, place: PlaceSummary) => Promise<void>;
  onReorder: (itemPublicIds: string[]) => Promise<void>;
  onSelectItem: (itemPublicId: string | null) => void;
};

const issueLabels: Record<ItineraryIssueType, string> = {
  PLACE_CLOSED: "Địa điểm đóng cửa",
  OPENING_HOURS_UNKNOWN: "Chưa có thông tin giờ mở cửa",
  ENDS_AFTER_CLOSING: "Lịch tham quan kết thúc sau giờ đóng cửa",
  ENDS_AFTER_TRIP: "Lịch trình vượt quá thời gian chuyến đi",
  OVER_BUDGET: "Lịch trình vượt ngân sách",
};

const distanceFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 1,
});

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date(`${value}T00:00:00`));
}

function formatTime(value: string) {
  return value.slice(0, 5);
}

function formatDistance(value: number) {
  return `${distanceFormatter.format(value)} km`;
}

export function ItineraryView({
  trip,
  itinerary,
  loading,
  error,
  selectedItemPublicId,
  mutating,
  onRetry,
  onAdd,
  onDelete,
  onReplace,
  onReorder,
  onSelectItem,
}: ItineraryViewProps) {
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [picker, setPicker] = useState<PickerState | null>(null);

  const items = [...(itinerary?.items ?? [])].sort(
    (left, right) => left.sequenceNo - right.sequenceNo,
  );
  const excludedPlaceSlugs = items.map((item) => item.place.slug);

  async function handleDelete(itemPublicId: string, placeName: string) {
    setOpenMenuId(null);
    const confirmed = window.confirm(
      `Xóa “${placeName}” khỏi hành trình?`,
    );
    if (!confirmed) return;

    try {
      await onDelete(itemPublicId);
      if (selectedItemPublicId === itemPublicId) {
        onSelectItem(null);
      }
    } catch {
      // The parent view renders the API error state.
    }
  }

  async function handleMove(index: number, offset: -1 | 1) {
    setOpenMenuId(null);
    const targetIndex = index + offset;
    if (targetIndex < 0 || targetIndex >= items.length) return;

    const itemPublicIds = items.map((item) => item.publicId);
    [itemPublicIds[index], itemPublicIds[targetIndex]] = [
      itemPublicIds[targetIndex],
      itemPublicIds[index],
    ];

    try {
      await onReorder(itemPublicIds);
    } catch {
      // The parent view renders the API error state.
    }
  }

  async function handlePlaceSelect(place: PlaceSummary) {
    if (!picker) return;

    if (picker.mode === "add") {
      await onAdd(place);
      return;
    }

    if (!picker.itemPublicId) return;
    await onReplace(picker.itemPublicId, place);
  }

  if (loading) {
    return (
      <div className="grid gap-4" aria-label="Đang tải hành trình">
        <Skeleton className="h-28 rounded-mint-md" />
        <Skeleton className="h-24 rounded-mint-md" />
        <Skeleton className="h-24 rounded-mint-md" />
      </div>
    );
  }

  return (
    <article>
      <header>
        <p className="m-0 text-sm font-extrabold tracking-[0.14em] text-primary uppercase">
          Hành trình
        </p>
        <h1 className="mt-2 mb-0 text-[clamp(2rem,4vw,3rem)] leading-[1.08] font-bold tracking-[-0.05em] capitalize">
          {formatDate(trip.tripDate)}
        </h1>
        <p className="mt-3 mb-0 text-sm leading-6 text-text-secondary">
          Quản lý thứ tự ghé thăm và xem lịch trình được cập nhật sau mỗi thay
          đổi.
        </p>
      </header>

      <section className="mt-8" aria-label="Điểm xuất phát">
        <div className="grid grid-cols-[36px_1fr] items-start gap-3 rounded-xl border border-border bg-surface p-4">
          <span className="grid size-9 place-items-center rounded-full bg-primary-soft text-primary">
            <MapPin className="size-4" aria-hidden="true" />
          </span>
          <div>
            <p className="m-0 text-xs font-extrabold tracking-[0.1em] text-text-secondary uppercase">
              Điểm xuất phát
            </p>
            <p className="mt-1 mb-0 font-bold text-text-primary">
              {trip.startLocation.label}
            </p>
          </div>
        </div>
      </section>

      {error ? (
        <section className="mt-5 rounded-xl border border-destructive/25 bg-destructive/8 p-4">
          <p className="m-0 text-sm font-semibold text-destructive">{error}</p>
          <Button
            type="button"
            size="sm"
            variant="outline"
            className="mt-3"
            onClick={onRetry}
            disabled={mutating}
          >
            Tải lại hành trình
          </Button>
        </section>
      ) : null}

      {itinerary ? (
        <>
          <section className="mt-6" aria-labelledby="itinerary-summary-heading">
            <h2
              id="itinerary-summary-heading"
              className="m-0 text-xs font-extrabold tracking-[0.12em] text-text-secondary uppercase"
            >
              Tổng quan lịch trình
            </h2>
            <dl className="mt-3 grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-border bg-border sm:grid-cols-4">
              <div className="bg-surface p-2.5">
                <dt className="text-xs text-text-secondary">Chi phí</dt>
                <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
                  {formatCurrency(itinerary.summary.totalEstimatedCost)}
                </dd>
              </div>
              <div className="bg-surface p-2.5">
                <dt className="text-xs text-text-secondary">Di chuyển</dt>
                <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
                  {formatDuration(itinerary.summary.totalTravelMinutes)}
                </dd>
              </div>
              <div className="bg-surface p-2.5">
                <dt className="text-xs text-text-secondary">Tham quan</dt>
                <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
                  {formatDuration(itinerary.summary.totalVisitMinutes)}
                </dd>
              </div>
              <div className="bg-surface p-2.5">
                <dt className="text-xs text-text-secondary">Quãng đường</dt>
                <dd className="mt-1 text-sm font-bold text-text-primary tabular-nums">
                  {formatDistance(itinerary.summary.totalDistanceKm)}
                </dd>
              </div>
            </dl>
          </section>

          {itinerary.issues.length > 0 ? (
            <section className="mt-5" aria-labelledby="itinerary-issues-heading">
              <h2 id="itinerary-issues-heading" className="sr-only">
                Lưu ý lịch trình
              </h2>
              <ul className="divide-y divide-ochre/20 overflow-hidden rounded-xl border border-ochre/30 bg-ochre-soft">
                {itinerary.issues.map((issue, index) => {
                  const placeName = issue.placeSlug
                    ? items.find((item) => item.place.slug === issue.placeSlug)
                        ?.place.name
                    : null;

                  return (
                    <li
                      key={`${issue.type}-${issue.placeSlug ?? "trip"}-${index}`}
                      className="flex items-start gap-2 px-3 py-2.5 text-sm text-ochre-foreground"
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
          ) : null}

          <section className="mt-6" aria-labelledby="itinerary-items-heading">
            <div className="mb-3 flex items-center justify-between gap-3">
              <div>
                <h2
                  id="itinerary-items-heading"
                  className="m-0 text-xs font-extrabold tracking-[0.12em] text-text-secondary uppercase"
                >
                  Địa điểm trong hành trình
                </h2>
                <p className="mt-1 mb-0 text-xs text-text-secondary">
                  {items.length === 0
                    ? "Chưa có địa điểm"
                    : `${items.length} địa điểm`}
                </p>
              </div>

              <Button
                type="button"
                size="sm"
                variant="accent"
                onClick={() => setPicker({ mode: "add", itemPublicId: null })}
                disabled={mutating}
              >
                <Plus className="size-4" aria-hidden="true" />
                Thêm địa điểm
              </Button>
            </div>

            {items.length === 0 ? (
              <div className="grid min-h-48 place-items-center rounded-mint-md border border-dashed border-border bg-surface/60 p-6 text-center">
                <div className="max-w-xs">
                  <span className="mx-auto grid size-12 place-items-center rounded-full bg-primary-soft text-primary">
                    <MapPin className="size-5" aria-hidden="true" />
                  </span>
                  <h3 className="mt-4 mb-0 text-lg font-bold">
                    Hành trình đang trống
                  </h3>
                  <p className="mt-2 mb-0 text-sm leading-6 text-text-secondary">
                    Thêm địa điểm đầu tiên để bắt đầu xây dựng chuyến đi của bạn.
                  </p>
                  <Button
                    type="button"
                    variant="outline"
                    className="mt-4"
                    onClick={() =>
                      setPicker({ mode: "add", itemPublicId: null })
                    }
                    disabled={mutating}
                  >
                    <Plus className="size-4" aria-hidden="true" />
                    Thêm địa điểm
                  </Button>
                </div>
              </div>
            ) : (
              <ol className="grid gap-3">
                {items.map((item, index) => {
                  const selected = item.publicId === selectedItemPublicId;
                  const menuOpen = openMenuId === item.publicId;

                  return (
                    <li
                      key={item.publicId}
                      className={`relative ${
                        index < items.length - 1
                          ? "after:absolute after:top-full after:left-8 after:h-3 after:w-px after:bg-primary/30 after:content-['']"
                          : ""
                      }`}
                    >
                      <div
                        className={`grid grid-cols-[minmax(0,1fr)_40px] items-stretch rounded-xl border p-1 transition ${
                          selected
                            ? "border-primary/55 bg-primary-soft/35 shadow-mint-sm"
                            : "border-border bg-surface hover:border-primary/35"
                        }`}
                      >
                        <button
                          type="button"
                          className="grid min-w-0 grid-cols-[42px_1fr] items-start gap-3 rounded-lg p-2 text-left"
                          onClick={() => onSelectItem(item.publicId)}
                        >
                          <span className="grid size-10 place-items-center rounded-full bg-primary text-sm font-black text-primary-foreground">
                            {item.sequenceNo}
                          </span>

                          <span className="min-w-0">
                            <strong className="block truncate text-base text-text-primary">
                              {item.place.name}
                            </strong>
                            <span className="mt-1.5 flex items-center gap-1 text-[0.8rem] leading-5 font-semibold text-text-primary tabular-nums">
                              <Clock3
                                className="size-3.5 text-primary"
                                aria-hidden="true"
                              />
                              {formatTime(item.schedule.visitStartTime)} –{" "}
                              {formatTime(item.schedule.visitEndTime)}
                            </span>
                            <span className="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-xs leading-5 text-text-secondary">
                              <span className="tabular-nums">
                                Đến {formatTime(item.schedule.arrivalTime)}
                              </span>
                              <span className="inline-flex items-center gap-1 tabular-nums">
                                <Route className="size-3.5" aria-hidden="true" />
                                {item.schedule.travelMinutes} phút ·{" "}
                                {formatDistance(item.schedule.travelDistanceKm)}
                              </span>
                              <span className="inline-flex items-center gap-1 text-ochre-foreground tabular-nums">
                                <WalletCards
                                  className="size-3.5 text-ochre"
                                  aria-hidden="true"
                                />
                                {formatCurrency(item.schedule.estimatedCost)}
                              </span>
                            </span>
                          </span>
                        </button>

                        <button
                          type="button"
                          className="m-auto grid size-9 place-items-center rounded-lg text-text-secondary transition hover:bg-muted hover:text-text-primary"
                          aria-label={`Tùy chọn cho ${item.place.name}`}
                          aria-expanded={menuOpen}
                          disabled={mutating}
                          onClick={() =>
                            setOpenMenuId(menuOpen ? null : item.publicId)
                          }
                        >
                          <MoreHorizontal className="size-5" aria-hidden="true" />
                        </button>
                      </div>

                      {menuOpen ? (
                        <div className="absolute top-12 right-3 z-20 min-w-48 rounded-xl border border-border bg-popover p-1.5 shadow-mint-md">
                          <button
                            type="button"
                            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
                            onClick={() => handleMove(index, -1)}
                            disabled={mutating || index === 0}
                          >
                            <ArrowUp className="size-4" aria-hidden="true" />
                            Di chuyển lên
                          </button>
                          <button
                            type="button"
                            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
                            onClick={() => handleMove(index, 1)}
                            disabled={mutating || index === items.length - 1}
                          >
                            <ArrowDown className="size-4" aria-hidden="true" />
                            Di chuyển xuống
                          </button>
                          <button
                            type="button"
                            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
                            onClick={() => {
                              setOpenMenuId(null);
                              setPicker({
                                mode: "replace",
                                itemPublicId: item.publicId,
                              });
                            }}
                            disabled={mutating}
                          >
                            <PencilLine className="size-4" aria-hidden="true" />
                            Thay địa điểm
                          </button>
                          <button
                            type="button"
                            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold text-destructive hover:bg-destructive/10 disabled:cursor-not-allowed disabled:opacity-45"
                            onClick={() =>
                              handleDelete(item.publicId, item.place.name)
                            }
                            disabled={mutating}
                          >
                            <Trash2 className="size-4" aria-hidden="true" />
                            Xóa khỏi hành trình
                          </button>
                        </div>
                      ) : null}
                    </li>
                  );
                })}
              </ol>
            )}
          </section>
        </>
      ) : null}

      <PlacePickerSheet
        open={picker !== null}
        mode={picker?.mode ?? "add"}
        excludedPlaceSlugs={excludedPlaceSlugs}
        busy={mutating}
        onOpenChange={(open) => {
          if (!open) setPicker(null);
        }}
        onSelect={handlePlaceSelect}
      />
    </article>
  );
}
