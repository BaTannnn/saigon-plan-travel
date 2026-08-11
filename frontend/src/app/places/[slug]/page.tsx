import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PlaceDetailView } from "@/features/places/components/place-detail-view";
import { ApiError } from "@/lib/api/api-client";
import { getPlaceDetail } from "@/lib/api/place-api";

export const metadata: Metadata = {
  title: "Chi tiết địa điểm",
};

async function loadPlaceDetail(slug: string) {
  try {
    return await getPlaceDetail(slug);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export default async function PlaceDetailPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const place = await loadPlaceDetail(slug);

  if (!place) {
    notFound();
  }

  return <PlaceDetailView place={place} />;
}
