"use client";

import dynamic from "next/dynamic";
import { Skeleton } from "@/components/ui/skeleton";
import type { PlaceSummary } from "@/types/place";

const PlaceMap = dynamic(
  () => import("@/features/map/place-map").then((module) => module.PlaceMap),
  {
    ssr: false,
    loading: () => (
      <Skeleton
        className="flex size-full items-center justify-center gap-2.5 bg-[linear-gradient(135deg,var(--primary-soft),var(--background))] text-primary-strong"
        role="status"
      >
        <span className="size-6 animate-spin rounded-full border-[3px] border-border border-t-primary motion-reduce:animate-none" />
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
