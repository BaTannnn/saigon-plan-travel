"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";
import { TripMonthBoard } from "@/features/trips/components/trip-month-board";
import { TripMonthToolbar } from "@/features/trips/components/trip-month-toolbar";
import {
  getTripDeleteErrorMessage,
  getTripsLoadErrorMessage,
} from "@/features/trips/trip-errors";
import { ApiError } from "@/lib/api/api-client";
import { deleteTrip, getTrips } from "@/lib/api/trip-api";
import type { TripSummaryResponse } from "@/types/trip";

function getCurrentMonth() {
  const today = new Date();
  return { year: today.getFullYear(), month: today.getMonth() + 1 };
}

function formatDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);

  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(year, month - 1, day));
}

export function TripsHubView() {
  const router = useRouter();
  const { status, runAuthenticated } = useAuth();
  const [initialMonth] = useState(getCurrentMonth);
  const [selectedYear, setSelectedYear] = useState(initialMonth.year);
  const [selectedMonth, setSelectedMonth] = useState(initialMonth.month);
  const [years] = useState(() =>
    Array.from({ length: 7 }, (_, index) => initialMonth.year - 1 + index),
  );
  const [trips, setTrips] = useState<TripSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
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

    runAuthenticated((token) =>
      getTrips({ year: selectedYear, month: selectedMonth }, token),
    )
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
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [router, runAuthenticated, selectedMonth, selectedYear, status]);

  const deleting = deletingPublicId !== null;
  const firstYear = years[0];
  const lastYear = years[years.length - 1];
  const previousDisabled = selectedYear === firstYear && selectedMonth === 1;
  const nextDisabled = selectedYear === lastYear && selectedMonth === 12;

  function moveMonth(offset: number) {
    const nextDate = new Date(selectedYear, selectedMonth - 1 + offset, 1);
    setLoading(true);
    setLoadError(null);
    setTrips([]);
    setSelectedYear(nextDate.getFullYear());
    setSelectedMonth(nextDate.getMonth() + 1);
  }

  function selectMonth(month: number) {
    setLoading(true);
    setLoadError(null);
    setTrips([]);
    setSelectedMonth(month);
  }

  function selectYear(year: number) {
    setLoading(true);
    setLoadError(null);
    setTrips([]);
    setSelectedYear(year);
  }

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
        current.filter((trip) => trip.publicId !== publicId),
      );
      setTripPendingDelete(null);
    } catch (error) {
      setDeleteError(getTripDeleteErrorMessage(error));
    } finally {
      setDeletingPublicId(null);
    }
  }

  if (status !== "authenticated") {
    return (
      <main
        className="mx-auto grid w-[min(1120px,calc(100%_-_32px))] gap-4 py-10"
        aria-label="Đang tải danh sách chuyến đi"
      >
        <Skeleton className="h-24 rounded-mint-lg" />
        <div className="grid grid-cols-4 gap-3 max-md:grid-cols-2 max-sm:grid-cols-1">
          {Array.from({ length: 8 }, (_, index) => (
            <Skeleton key={index} className="aspect-square rounded-mint-md" />
          ))}
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto w-[min(1440px,calc(100%_-_32px))] py-8 pb-16 max-md:py-6">
      <header className="mb-6 grid gap-4">
        <h1 className="m-0 text-3xl leading-tight font-bold tracking-[-0.04em] max-md:text-2xl">
          Chuyến đi của tôi
        </h1>

        <div className="flex items-center justify-between gap-4 max-sm:flex-col max-sm:items-stretch">
          <TripMonthToolbar
            month={selectedMonth}
            year={selectedYear}
            years={years}
            previousDisabled={previousDisabled}
            nextDisabled={nextDisabled}
            onMonthChange={selectMonth}
            onYearChange={selectYear}
            onPreviousMonth={() => moveMonth(-1)}
            onNextMonth={() => moveMonth(1)}
          />
          <Button asChild variant="accent" className="shrink-0">
            <Link href="/trips/new">Tạo chuyến đi</Link>
          </Button>
        </div>
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
        <Alert className="mb-4 rounded-mint-md border-border bg-surface shadow-mint-sm">
          <AlertTitle>Không thể tải chuyến đi</AlertTitle>
          <AlertDescription>{loadError}</AlertDescription>
        </Alert>
      ) : null}

      {!loadError ? (
        <TripMonthBoard
          year={selectedYear}
          month={selectedMonth}
          trips={trips}
          loading={loading}
          onDelete={(selectedTrip) => {
            setDeleteError(null);
            setTripPendingDelete(selectedTrip);
          }}
        />
      ) : null}
    </main>
  );
}
