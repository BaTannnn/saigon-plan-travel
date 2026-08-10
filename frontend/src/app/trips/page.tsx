import type { Metadata } from "next";
import { TripsHubView } from "@/features/trips/components/trips-hub-view";

export const metadata: Metadata = {
  title: "Chuyến đi của tôi",
};

export default function TripsPage() {
  return <TripsHubView />;
}
