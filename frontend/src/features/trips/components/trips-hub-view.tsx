"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";
import { getTripsLoadErrorMessage } from "@/features/trips/trip-errors";
import { ApiError } from "@/lib/api/api-client";
import { getTrips } from "@/lib/api/trip-api";
import type {
  EnvironmentPreference,
  TravelPace,
  TripSummaryResponse,
} from "@/types/trip";

const paceLabels: Record<TravelPace, string> = {
  RELAXED: "Thư thả",
  BALANCED: "Cân bằng",
  FAST: "Nhanh",
};

const environmentLabels: Record<EnvironmentPreference, string> = {
  INDOOR: "Trong nhà",
  OUTDOOR: "Ngoài trời",
  MIXED: "Kết hợp",
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 2,
  }).format(value);
}

function TripCard({ trip }: { trip: TripSummaryResponse }) {
  return (
    <Card
      asChild
      className="rounded-mint-lg border-border bg-surface p-5 shadow-mint-sm ring-0 transition-transform hover:-translate-y-0.5 hover:shadow-mint-md"
    >
      <Link href={`/trips/${trip.publicId}`}>
        <div className="flex items-start justify-between gap-4 max-sm:flex-col">
          <div>
            <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
              Chuyến đi đã lưu
            </p>
            <h2 className="mt-1.5 mb-1 text-xl font-bold tracking-[-0.025em] capitalize">
              {formatDate(trip.tripDate)}
            </h2>
            <p className="m-0 font-bold text-primary-strong">
              {trip.startTime} – {trip.endTime}
            </p>
          </div>
          <p className="m-0 text-lg font-bold whitespace-nowrap">
            {formatMoney(trip.budget)}
          </p>
        </div>

        <p className="mt-4 mb-0 text-sm text-text-secondary">
          Xuất phát: {trip.startLocationLabel}
        </p>
        <p className="mt-1 mb-0 text-sm text-text-secondary">
          {paceLabels[trip.travelPace]} ·{" "}
          {environmentLabels[trip.environmentPreference]}
        </p>

      </Link>
    </Card>
  );
}

export function TripsHubView() {
  const router = useRouter();
  const { status, runAuthenticated } = useAuth();
  const [trips, setTrips] = useState<TripSummaryResponse[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "guest") {
      router.replace("/login");
      return;
    }
    if (status !== "authenticated") return;

    let cancelled = false;

    runAuthenticated((token) => getTrips(token))
      .then((response) => {
        if (cancelled) return;
        setTrips(response);
      })
      .catch((error) => {
        if (cancelled) return;
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError(getTripsLoadErrorMessage(error));
      });

    return () => {
      cancelled = true;
    };
  }, [router, runAuthenticated, status]);

  if (status !== "authenticated" || (!trips && !loadError)) {
    return (
      <main
        className="mx-auto grid w-[min(900px,calc(100%_-_32px))] gap-4 py-10"
        aria-label="Đang tải danh sách chuyến đi"
      >
        <Skeleton className="h-32 rounded-mint-lg" />
        <Skeleton className="h-48 rounded-mint-md" />
        <Skeleton className="h-48 rounded-mint-md" />
      </main>
    );
  }

  return (
    <main className="mx-auto w-[min(900px,calc(100%_-_32px))] py-9 pb-16 max-md:py-6">
      <header className="mb-7 flex items-end justify-between gap-5 max-sm:items-start max-sm:flex-col">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            FE-F02.5 · Trip Hub
          </p>
          <h1 className="mt-1.5 mb-2 text-[clamp(2rem,5vw,3.4rem)] leading-[1.08] font-bold tracking-[-0.05em]">
            Chuyến đi của tôi
          </h1>
          <p className="m-0 max-w-2xl leading-7 text-text-secondary">
            Mở lại một chuyến đi đã lưu để xem hoặc chỉnh sửa sở thích.
          </p>
        </div>
        <Button asChild className="max-sm:w-full">
          <Link href="/trips/new">Tạo chuyến đi</Link>
        </Button>
      </header>

      {loadError ? (
        <Alert className="rounded-mint-lg border-border bg-surface p-6 shadow-mint-sm">
          <AlertTitle>Không thể tải chuyến đi</AlertTitle>
          <AlertDescription>{loadError}</AlertDescription>
        </Alert>
      ) : trips?.length === 0 ? (
        <Card className="items-start rounded-mint-lg border-border bg-[linear-gradient(145deg,var(--surface),var(--primary-soft))] p-7 shadow-mint-sm ring-0">
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Chưa có chuyến đi
          </p>
          <h2 className="m-0 text-2xl font-bold tracking-[-0.03em]">
            Bắt đầu với kế hoạch đầu tiên của bạn
          </h2>
          <p className="m-0 max-w-xl leading-7 text-text-secondary">
            Lưu ngày đi, khung giờ, ngân sách, điểm xuất phát và sở thích của chuyến đi.
          </p>
          <Button asChild variant="accent">
            <Link href="/trips/new">Tạo chuyến đi</Link>
          </Button>
        </Card>
      ) : (
        <div className="grid gap-4" aria-label="Các chuyến đi đã lưu">
          {trips?.map((trip) => (
            <TripCard key={trip.publicId} trip={trip} />
          ))}
        </div>
      )}
    </main>
  );
}
