import type { Metadata } from "next";
import { connection } from "next/server";
import { CreateTripView } from "@/features/trips/components/create-trip-view";

export const metadata: Metadata = {
  title: "Tạo chuyến đi",
};

export default async function NewTripPage() {
  await connection();
  return <CreateTripView />;
}
