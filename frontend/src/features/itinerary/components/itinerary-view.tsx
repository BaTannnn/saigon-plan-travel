"use client";

import { useState } from "react";
import { MapPin, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ItineraryGenerationSheet } from "@/features/itinerary/components/itinerary-generation-sheet";
import {
  ItineraryIssues,
  ItinerarySummary,
} from "@/features/itinerary/components/itinerary-insights";
import { ItineraryTimeline } from "@/features/itinerary/components/itinerary-timeline";
import {
  PlacePickerSheet,
  type PlacePickerMode,
} from "@/features/itinerary/components/place-picker-sheet";
import type {
  ItineraryGenerationPreviewResponse,
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
  onGeneratePreview: (
    preferenceDescription: string,
  ) => Promise<ItineraryGenerationPreviewResponse>;
  onApplyPreview: (placeSlugs: string[]) => Promise<void>;
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date(`${value}T00:00:00`));
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
  onGeneratePreview,
  onApplyPreview,
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
        <div className="mt-4">
          <ItineraryGenerationSheet
            onGenerate={onGeneratePreview}
            onApply={onApplyPreview}
          />
        </div>
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
          <ItinerarySummary
            summary={itinerary.summary}
            headingId="itinerary-summary-heading"
          />

          <ItineraryIssues
            issues={itinerary.issues}
            places={items.map((item) => item.place)}
            headingId="itinerary-issues-heading"
          />

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
              <ItineraryTimeline
                items={items}
                selectedItemPublicId={selectedItemPublicId}
                openMenuId={openMenuId}
                mutating={mutating}
                onSelectItem={onSelectItem}
                onToggleMenu={setOpenMenuId}
                onMove={handleMove}
                onReplace={(itemPublicId) => {
                  setOpenMenuId(null);
                  setPicker({ mode: "replace", itemPublicId });
                }}
                onDelete={handleDelete}
              />
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
