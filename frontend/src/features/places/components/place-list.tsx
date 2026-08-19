"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { PlaceCard } from "@/features/places/components/place-card";
import type { PlaceSummary } from "@/types/place";

type PlaceListProps = {
  places: PlaceSummary[];
  selectedSlug: string | null;
  onSelect: (slug: string) => void;
};

export function PlaceList({ places, selectedSlug, onSelect }: PlaceListProps) {
  useEffect(() => {
    if (!selectedSlug) return;
    document
      .getElementById(`place-card-${selectedSlug}`)
      ?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [selectedSlug]);

  if (places.length === 0) {
    return (
      <Card className="grid flex-1 content-center place-items-center border border-dashed border-border/60 bg-card px-5 py-9 text-center">
        <span className="text-5xl text-primary" aria-hidden="true">
          ⌁
        </span>
        <h2 className="mt-2 mb-1 text-xl font-bold">Không tìm thấy địa điểm</h2>
        <p className="mb-[18px] max-w-[340px] text-text-secondary">
          Thử đổi từ khóa hoặc xóa bớt bộ lọc để xem thêm kết quả.
        </p>
        <Button asChild variant="outline">
          <Link href="/places">Xóa bộ lọc</Link>
        </Button>
      </Card>
    );
  }

  return (
    <div
      className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto py-0 pr-[7px] pb-[18px] pl-0.5 overscroll-contain [scrollbar-color:var(--border)_transparent] max-md:overflow-visible"
      aria-label="Danh sách địa điểm"
    >
      {places.map((place, index) => (
        <PlaceCard
          key={place.id}
          place={place}
          index={index}
          selected={selectedSlug === place.slug}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}
