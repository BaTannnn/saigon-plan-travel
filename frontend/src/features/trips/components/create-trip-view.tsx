"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";
import { TripForm } from "@/features/trips/components/trip-form";
import { createTrip } from "@/lib/api/trip-api";
import type { SaveTripRequest } from "@/types/trip";

export function CreateTripView() {
  const router = useRouter();
  const { status, runAuthenticated } = useAuth();

  useEffect(() => {
    if (status === "guest") {
      router.replace("/login");
    }
  }, [router, status]);

  if (status !== "authenticated") {
    return (
      <main className="mx-auto grid w-[min(900px,calc(100%_-_32px))] gap-4 py-10" aria-label="Đang kiểm tra đăng nhập">
        <Skeleton className="h-24 rounded-mint-md" />
        <Skeleton className="h-80 rounded-mint-md" />
      </main>
    );
  }

  async function handleCreate(request: SaveTripRequest) {
    const trip = await runAuthenticated((token) => createTrip(request, token));
    router.push(`/trips/${trip.publicId}`);
  }

  return (
    <main className="mx-auto w-[min(780px,calc(100%_-_32px))] py-9 pb-16 max-md:py-6">
      <header className="mb-7">
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
          Chuyến đi một ngày
        </p>
        <h1 className="mt-1.5 mb-2 text-3xl leading-tight font-bold tracking-[-0.04em] max-md:text-2xl">
          Tạo chuyến đi
        </h1>
        <p className="m-0 max-w-2xl leading-7 text-text-secondary">
          Chọn thời gian, ngân sách, điểm xuất phát và sở thích cho một ngày khám phá của bạn.
        </p>
      </header>

      <TripForm submitLabel="Lưu chuyến đi" onSubmit={handleCreate} />
    </main>
  );
}
