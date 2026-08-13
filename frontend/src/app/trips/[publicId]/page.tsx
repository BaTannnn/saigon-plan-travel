import type { Metadata } from "next";
import { connection } from "next/server";
import { TripDetailView } from "@/features/trips/components/trip-detail-view";

export const metadata: Metadata = {
  title: "Chi tiết chuyến đi",
};

export default async function TripDetailPage({
  params,
}: {
  params: Promise<{ publicId: string }>;
}) {
  await connection();
  const { publicId } = await params;

  return <TripDetailView key={publicId} publicId={publicId} />;
}
