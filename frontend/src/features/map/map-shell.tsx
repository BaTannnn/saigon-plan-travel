"use client";

import dynamic from "next/dynamic";
import { Skeleton } from "@/components/ui/skeleton";
import type { PlaceSummary } from "@/types/place";

const PlaceMap = dynamic(
  () => import("@/features/map/place-map").then((module) => module.PlaceMap),
  {
    ssr: false,
    loading: () => (
      <Skeleton className="map-loading" role="status">
        <span className="loading-spinner" />
        Đang tải bản đồ…
      </Skeleton>
    ),
  },
);

type MapShellProps = {
  places: PlaceSummary[];
  selectedSlug?: string | null;
  onSelect?: (slug: string) => void;
  detailMode?: boolean;
};

export function MapShell(props: MapShellProps) {
  return <PlaceMap {...props} />;
}
