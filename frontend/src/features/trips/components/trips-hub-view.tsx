"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ClockIcon, PinIcon, WalletIcon } from "@/components/ui/icons";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";
import {
  getTripDeleteErrorMessage,
  getTripsLoadErrorMessage,
} from "@/features/trips/trip-errors";
import { ApiError } from "@/lib/api/api-client";
import { deleteTrip, getTrips } from "@/lib/api/trip-api";
import type { TripSummaryResponse } from "@/types/trip";
import { MoreHorizontal, Trash2 } from "lucide-react";

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

function getTileDate(value: string) {
  const date = new Date(`${value}T00:00:00`);

  return {
    day: new Intl.DateTimeFormat("vi-VN", { day: "numeric" }).format(date),
    weekday: new Intl.DateTimeFormat("vi-VN", { weekday: "long" }).format(date),
    month: new Intl.DateTimeFormat("vi-VN", { month: "long" }).format(date),
  };
}

function TripCalendarTile({
  trip,
  onDelete,
}: {
  trip: TripSummaryResponse;
  onDelete: (trip: TripSummaryResponse) => void;
}) {
  const date = getTileDate(trip.tripDate);

  return (
    <article className="relative">
      <Link
        href={`/trips/${trip.publicId}`}
        className="group relative flex min-h-64 flex-col rounded-xl border border-border bg-surface p-5 text-foreground transition-colors hover:border-primary/55 hover:bg-primary-soft/25 focus-visible:border-primary max-sm:min-h-0"
        aria-label={`Mở chuyến đi ${formatDate(trip.tripDate)}`}
      >
        <p className="m-0 pr-10 text-xs font-extrabold tracking-[0.11em] text-text-secondary uppercase">
          {date.weekday}
        </p>
        <div className="mt-4">
          <time
            dateTime={trip.tripDate}
            className="block text-6xl leading-none font-bold tracking-[-0.07em] text-primary-strong"
          >
            {date.day}
          </time>
          <p className="mt-1 mb-0 text-sm font-bold capitalize">{date.month}</p>
        </div>

        <div className="mt-auto grid gap-2.5 border-t border-border pt-4 text-sm">
          <p className="m-0 flex items-center gap-2 font-semibold text-text-primary">
            <ClockIcon className="size-4 text-primary" />
            {trip.startTime} – {trip.endTime}
          </p>
          <p className="m-0 flex items-center gap-2 font-semibold text-text-primary">
            <WalletIcon className="size-4 text-ochre" />
            {formatMoney(trip.budget)}
          </p>
          <p className="m-0 flex min-w-0 items-center gap-2 text-xs text-text-secondary">
            <PinIcon className="size-4 shrink-0 text-text-secondary" />
            <span className="truncate">{trip.startLocationLabel}</span>
          </p>
        </div>
      </Link>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            className="absolute top-2.5 right-2.5 z-10 size-9 bg-surface/90 text-text-secondary hover:bg-muted hover:text-text-primary"
            aria-label={`Tùy chọn cho chuyến đi ${formatDate(trip.tripDate)}`}
          >
            <MoreHorizontal className="size-5" aria-hidden="true" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem destructive onSelect={() => onDelete(trip)}>
            <Trash2 aria-hidden="true" />
            Xóa chuyến đi
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </article>
  );
}

export function TripsHubView() {
  const router = useRouter();
  const { status, runAuthenticated } = useAuth();
  const [trips, setTrips] = useState<TripSummaryResponse[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [tripPendingDelete, setTripPendingDelete] =
    useState<TripSummaryResponse | null>(null);
  const [deletingPublicId, setDeletingPublicId] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

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

  const deleting = deletingPublicId !== null;

  function handleDeleteDialogOpenChange(open: boolean) {
    if (open || deleting) return;
    setTripPendingDelete(null);
    setDeleteError(null);
  }

  async function handleDeleteTrip() {
    if (!tripPendingDelete || deleting) return;

    const publicId = tripPendingDelete.publicId;
    setDeletingPublicId(publicId);
    setDeleteError(null);

    try {
      await runAuthenticated((token) => deleteTrip(publicId, token));
      setTrips((current) =>
        current?.filter((trip) => trip.publicId !== publicId) ?? current,
      );
      setTripPendingDelete(null);
    } catch (error) {
      setDeleteError(getTripDeleteErrorMessage(error));
    } finally {
      setDeletingPublicId(null);
    }
  }

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
    <main className="mx-auto w-[min(1120px,calc(100%_-_32px))] py-9 pb-16 max-md:py-6">
      <header className="mb-7 flex items-end justify-between gap-5 max-sm:items-start max-sm:flex-col">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Chuyến đi
          </p>
          <h1 className="mt-1.5 mb-2 text-3xl leading-tight font-bold tracking-[-0.04em] max-md:text-2xl">
            Chuyến đi của tôi
          </h1>
          <p className="m-0 max-w-2xl leading-7 text-text-secondary">
            Những ngày bạn đã lên kế hoạch.
          </p>
        </div>
        <Button asChild variant="accent" className="max-sm:w-full">
          <Link href="/trips/new">Tạo chuyến đi</Link>
        </Button>
      </header>

      <AlertDialog
        open={tripPendingDelete !== null}
        onOpenChange={handleDeleteDialogOpenChange}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa chuyến đi?</AlertDialogTitle>
            <AlertDialogDescription>
              {tripPendingDelete
                ? `Chuyến đi ngày ${formatDate(tripPendingDelete.tripDate)} và hành trình đã lưu bên trong sẽ bị xóa vĩnh viễn.`
                : "Chuyến đi và hành trình đã lưu bên trong sẽ bị xóa vĩnh viễn."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteError ? (
            <Alert variant="destructive" role="alert">
              <AlertTitle>Không thể xóa chuyến đi</AlertTitle>
              <AlertDescription>{deleteError}</AlertDescription>
            </Alert>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel asChild>
              <Button type="button" variant="outline" disabled={deleting}>
                Hủy
              </Button>
            </AlertDialogCancel>
            <AlertDialogAction asChild>
              <Button
                type="button"
                variant="destructive"
                disabled={deleting}
                onClick={(event) => {
                  event.preventDefault();
                  void handleDeleteTrip();
                }}
              >
                {deleting ? "Đang xóa..." : "Xóa chuyến đi"}
              </Button>
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {loadError ? (
        <Alert className="rounded-mint-lg border-border bg-surface p-6 shadow-mint-sm">
          <AlertTitle>Không thể tải chuyến đi</AlertTitle>
          <AlertDescription>{loadError}</AlertDescription>
        </Alert>
      ) : trips?.length === 0 ? (
        <Card className="items-start rounded-xl border border-border bg-surface p-7 shadow-none ring-0">
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
        <div
          className="grid grid-cols-4 gap-4 max-xl:grid-cols-3 max-md:grid-cols-2 max-sm:grid-cols-1"
          aria-label="Các chuyến đi đã lưu"
        >
          {trips?.map((trip) => (
            <TripCalendarTile
              key={trip.publicId}
              trip={trip}
              onDelete={(selectedTrip) => {
                setDeleteError(null);
                setTripPendingDelete(selectedTrip);
              }}
            />
          ))}
        </div>
      )}
    </main>
  );
}
