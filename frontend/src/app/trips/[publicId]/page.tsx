import type { Metadata } from "next";
import { connection } from "next/server";
import { TripDetailView } from "@/features/trips/components/trip-detail-view";
import { getCategories } from "@/lib/api/category-api";

export const metadata: Metadata = {
  title: "Chi tiết chuyến đi",
};

export default async function TripDetailPage({
  params,
}: {
  params: Promise<{ publicId: string }>;
}) {
  await connection();
  const [{ publicId }, categories] = await Promise.all([params, getCategories()]);

  return (
    <TripDetailView
      key={publicId}
      publicId={publicId}
      categories={categories}
    />
  );
}
