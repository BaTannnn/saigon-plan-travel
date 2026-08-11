import type { Metadata } from "next";
import { ExploreLanding } from "@/features/places/components/explore-landing";

export const metadata: Metadata = {
  title: "Khám phá",
  description:
    "Tạo chuyến đi và xây dựng hành trình du lịch TP.HCM với SaigonPlanTravel.",
};

export default function PlacesPage() {
  return <ExploreLanding />;
}
