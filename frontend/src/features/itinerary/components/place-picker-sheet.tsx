"use client";

import { useEffect, useMemo, useState } from "react";
import { MapPin, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { getPlaceCatalog } from "@/lib/api/place-api";
import type { PlaceSummary } from "@/types/place";

export type PlacePickerMode = "add" | "replace";

type PlacePickerSheetProps = {
  open: boolean;
  mode: PlacePickerMode;
  excludedPlaceIds: number[];
  busy?: boolean;
  onOpenChange: (open: boolean) => void;
  onSelect: (place: PlaceSummary) => Promise<void>;
};

function normalize(value: string) {
  return value.trim().toLocaleLowerCase("vi-VN");
}

export function PlacePickerSheet({
  open,
  mode,
  excludedPlaceIds,
  busy = false,
  onOpenChange,
  onSelect,
}: PlacePickerSheetProps) {
  const [places, setPlaces] = useState<PlaceSummary[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);

  useEffect(() => {
    if (!open || places.length > 0) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    getPlaceCatalog()
      .then((response) => {
        if (cancelled) return;
        setPlaces(response.content);
      })
      .catch(() => {
        if (cancelled) return;
        setError("Không thể tải danh sách địa điểm. Vui lòng thử lại.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, places.length]);

  useEffect(() => {
    if (!open) {
      setQuery("");
      setSelectedPlaceId(null);
      setError(null);
    }
  }, [open]);

  const visiblePlaces = useMemo(() => {
    const excluded = new Set(excludedPlaceIds);
    const normalizedQuery = normalize(query);

    return places.filter((place) => {
      if (excluded.has(place.id)) return false;
      if (!normalizedQuery) return true;

      return [
        place.name,
        place.shortDescription ?? "",
        place.administrativeUnitName ?? "",
      ]
        .join(" ")
        .toLocaleLowerCase("vi-VN")
        .includes(normalizedQuery);
    });
  }, [excludedPlaceIds, places, query]);

  async function handleSelect(place: PlaceSummary) {
    setSelectedPlaceId(place.id);
    setError(null);

    try {
      await onSelect(place);
      onOpenChange(false);
    } catch {
      setError(
        mode === "add"
          ? "Không thể thêm địa điểm vào hành trình."
          : "Không thể thay địa điểm trong hành trình.",
      );
    } finally {
      setSelectedPlaceId(null);
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-[min(92vw,430px)] sm:max-w-[430px]">
        <SheetHeader className="border-b border-border px-5 py-5 pr-14">
          <SheetTitle className="text-lg font-bold">
            {mode === "add" ? "Thêm địa điểm" : "Thay địa điểm"}
          </SheetTitle>
          <SheetDescription>
            Chọn một địa điểm từ dữ liệu SaigonPlanTravel.
          </SheetDescription>
        </SheetHeader>

        <div className="px-5">
          <div className="relative">
            <Search
              className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-text-secondary"
              aria-hidden="true"
            />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="pl-9"
              placeholder="Tìm địa điểm..."
              aria-label="Tìm địa điểm"
            />
          </div>
        </div>

        {error ? (
          <p className="mx-5 rounded-lg bg-destructive/10 px-3 py-2 text-sm font-semibold text-destructive">
            {error}
          </p>
        ) : null}

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5">
          {loading ? (
            <div className="grid gap-3 py-2" aria-label="Đang tải địa điểm">
              {[1, 2, 3, 4].map((item) => (
                <div
                  key={item}
                  className="h-20 animate-pulse rounded-xl bg-muted/70"
                />
              ))}
            </div>
          ) : visiblePlaces.length === 0 ? (
            <div className="grid min-h-44 place-items-center text-center">
              <div>
                <p className="font-bold text-text-primary">
                  Không tìm thấy địa điểm phù hợp
                </p>
                <p className="mt-1 text-sm text-text-secondary">
                  Hãy thử một từ khóa khác.
                </p>
              </div>
            </div>
          ) : (
            <div className="grid gap-2 py-2">
              {visiblePlaces.map((place) => {
                const selecting = selectedPlaceId === place.id || busy;

                return (
                  <div
                    key={place.id}
                    className="grid grid-cols-[1fr_auto] items-center gap-3 rounded-xl border border-border bg-surface p-3"
                  >
                    <div className="min-w-0">
                      <div className="flex items-start gap-2">
                        <MapPin
                          className="mt-0.5 size-4 shrink-0 text-primary"
                          aria-hidden="true"
                        />
                        <div className="min-w-0">
                          <p className="m-0 truncate font-bold text-text-primary">
                            {place.name}
                          </p>
                          <p className="mt-1 mb-0 truncate text-xs text-text-secondary">
                            {place.administrativeUnitName ?? "TP.HCM"}
                            {place.indoor ? " · Trong nhà" : " · Ngoài trời"}
                          </p>
                        </div>
                      </div>
                    </div>

                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={selecting}
                      onClick={() => handleSelect(place)}
                    >
                      {selectedPlaceId === place.id
                        ? "Đang lưu..."
                        : mode === "add"
                          ? "Thêm"
                          : "Chọn"}
                    </Button>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
