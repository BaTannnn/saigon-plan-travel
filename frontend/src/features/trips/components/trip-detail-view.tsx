"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";
import { TripForm } from "@/features/trips/components/trip-form";
import { TripReview } from "@/features/trips/components/trip-review";
import { getTripLoadErrorMessage } from "@/features/trips/trip-errors";
import { TripOriginMapShell } from "@/features/trips/map/trip-origin-map-shell";
import { ApiError } from "@/lib/api/api-client";
import { getTrip, replaceTrip } from "@/lib/api/trip-api";
import type { Category } from "@/types/category";
import type { SaveTripRequest, TripResponse } from "@/types/trip";

type TripDetailViewProps = {
  publicId: string;
  categories: Category[];
};

export function TripDetailView({ publicId, categories }: TripDetailViewProps) {
  const router = useRouter();
  const { status, runAuthenticated } = useAuth();
  const [trip, setTrip] = useState<TripResponse | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "guest") {
      router.replace("/login");
      return;
    }
    if (status !== "authenticated") return;

    let cancelled = false;

    runAuthenticated((token) => getTrip(publicId, token))
      .then((response) => {
        if (cancelled) return;
        setTrip(response);
      })
      .catch((error) => {
        if (cancelled) return;
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError(getTripLoadErrorMessage(error));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [publicId, router, runAuthenticated, status]);

  async function handleReplace(request: SaveTripRequest) {
    const updated = await runAuthenticated((token) =>
      replaceTrip(publicId, request, token),
    );
    setTrip(updated);
    setEditing(false);
  }

  if (status === "loading" || loading) {
    return (
      <main
        className="mx-auto grid w-[min(1120px,calc(100%_-_32px))] gap-4 py-10"
        aria-label="Đang tải chuyến đi"
      >
        <Skeleton className="h-52 rounded-mint-lg" />
        <Skeleton className="h-64 rounded-mint-md" />
      </main>
    );
  }

  if (loadError || !trip) {
    return (
      <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center p-5 max-md:min-h-[calc(100dvh_-_4rem)]">
        <Alert className="max-w-xl rounded-mint-lg border-border bg-surface p-6 shadow-mint-md">
          <AlertTitle>Không thể mở chuyến đi</AlertTitle>
          <AlertDescription>
            {loadError ?? "Chuyến đi không khả dụng."}
          </AlertDescription>
          <div className="mt-5 flex gap-3">
            <Button asChild>
              <Link href="/trips/new">Tạo chuyến đi mới</Link>
            </Button>
            <Button asChild variant="outline">
              <Link href="/places">Về khám phá</Link>
            </Button>
          </div>
        </Alert>
      </main>
    );
  }

  const centerContent = editing ? (
    <>
      <header className="mb-7">
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
          PUT · Thay thế đầy đủ
        </p>
        <h1 className="mt-1.5 mb-2 text-[clamp(2rem,5vw,3.2rem)] leading-[1.08] font-bold tracking-[-0.05em]">
          Chỉnh sửa chuyến đi
        </h1>
        <p className="m-0 text-text-secondary">
          Khi lưu, toàn bộ trường bên dưới sẽ thay thế phiên bản hiện tại.
        </p>
      </header>
      <TripForm
        categories={categories}
        initialTrip={trip}
        submitLabel="Lưu thay đổi"
        onSubmit={handleReplace}
        onCancel={() => setEditing(false)}
      />
    </>
  ) : (
    <TripReview trip={trip} onEdit={() => setEditing(true)} />
  );

  return (
    <main className="grid min-h-[calc(100dvh_-_4rem)] grid-cols-1 bg-background md:min-h-[calc(100dvh_-_5rem)] xl:h-[calc(100dvh_-_5rem)] xl:min-h-[680px] xl:grid-cols-[clamp(560px,52vw,760px)_minmax(0,1fr)] xl:overflow-hidden">
      <section className="min-w-0 px-5 py-8 sm:px-8 md:px-10 xl:min-h-0 xl:overflow-y-auto xl:px-12 xl:py-10">
        {centerContent}
      </section>
      <section
        className="h-[360px] min-w-0 border-t border-border md:h-[420px] xl:h-auto xl:min-h-0 xl:border-t-0 xl:border-l"
        aria-label="Bản đồ điểm xuất phát"
      >
        <TripOriginMapShell
          latitude={trip.startLocation.latitude}
          longitude={trip.startLocation.longitude}
          label={trip.startLocation.label}
        />
      </section>
    </main>
  );
}
