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
      <Card className="empty-state">
        <span aria-hidden="true">⌁</span>
        <h2>Không tìm thấy địa điểm</h2>
        <p>Thử đổi từ khóa hoặc xóa bớt bộ lọc để xem thêm kết quả.</p>
        <Button asChild variant="outline">
          <Link href="/places">Xóa bộ lọc</Link>
        </Button>
      </Card>
    );
  }

  return (
    <div className="place-list" aria-label="Danh sách địa điểm">
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
