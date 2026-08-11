"use client";

import dynamic from "next/dynamic";
import { Skeleton } from "@/components/ui/skeleton";
import type { ItineraryItemResponse } from "@/types/itinerary";

const ItineraryMap = dynamic(
  () =>
    import("@/features/itinerary/map/itinerary-map").then(
      (module) => module.ItineraryMap,
    ),
  {
    ssr: false,
    loading: () => <Skeleton className="size-full rounded-none" />,
  },
);

type ItineraryMapShellProps = {
  origin: {
    label: string;
    latitude: number;
    longitude: number;
  };
  items: ItineraryItemResponse[];
  selectedItemPublicId: string | null;
  onSelectItem: (itemPublicId: string) => void;
};

export function ItineraryMapShell(props: ItineraryMapShellProps) {
  return <ItineraryMap {...props} />;
}
