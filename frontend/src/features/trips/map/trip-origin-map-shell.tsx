"use client";

import dynamic from "next/dynamic";
import { Skeleton } from "@/components/ui/skeleton";

const TripOriginMap = dynamic(
  () =>
    import("@/features/trips/map/trip-origin-map").then(
      (module) => module.TripOriginMap,
    ),
  {
    ssr: false,
    loading: () => (
      <Skeleton
        className="flex size-full items-center justify-center gap-2.5 rounded-none bg-primary-soft text-primary-strong"
        role="status"
      >
        <span className="size-6 animate-spin rounded-full border-[3px] border-border border-t-primary motion-reduce:animate-none" />
        Đang tải bản đồ…
      </Skeleton>
    ),
  },
);

type TripOriginMapShellProps = {
  latitude: number;
  longitude: number;
  label: string;
};

export function TripOriginMapShell(props: TripOriginMapShellProps) {
  return <TripOriginMap {...props} />;
}
