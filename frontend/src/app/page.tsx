import type { Metadata } from "next";
import { HomeLanding } from "@/features/home/components/home-landing";

export const metadata: Metadata = {
  title: "Khám phá",
  description:
    "Tạo chuyến đi và xây dựng hành trình du lịch TP.HCM với SaigonPlanTravel.",
};

export default function Home() {
  return <HomeLanding />;
}
