"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { formatCurrency, formatTime } from "@/lib/formatters";
import { cn } from "@/lib/utils";
import type { TripSummaryResponse } from "@/types/trip";
import { CalendarDays, MoreHorizontal, Plus, Trash2 } from "lucide-react";

type TripMonthBoardProps = {
  year: number;
  month: number;
  trips: TripSummaryResponse[];
  loading: boolean;
  onDelete: (trip: TripSummaryResponse) => void;
};

const weekdayFormatter = new Intl.DateTimeFormat("vi-VN", {
  weekday: "long",
});

function getTripDay(tripDate: string) {
  return Number(tripDate.slice(8, 10));
}

function formatDate(year: number, month: number, day: number) {
  return new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(year, month - 1, day));
}

function TripEntry({
  trip,
  onDelete,
  variant = "day-cell",
}: {
  trip: TripSummaryResponse;
  onDelete: (trip: TripSummaryResponse) => void;
  variant?: "day-cell" | "popover";
}) {
  return (
    <div
      className={cn(
        "group/trip relative transition-colors",
        variant === "day-cell"
          ? "rounded-lg border border-border/70 bg-surface px-2.5 py-2 pr-10 hover:border-primary/35 hover:bg-primary-soft/30"
          : "px-1 py-3 pr-10 hover:bg-muted/45",
      )}
    >
      <Link
        href={`/trips/${trip.publicId}`}
        className="block min-w-0 rounded-sm"
        aria-label={`Mở chuyến đi ngày ${trip.tripDate}, ${formatTime(trip.startTime)} đến ${formatTime(trip.endTime)}`}
      >
        <p className="m-0 text-xs font-bold text-text-primary">
          {formatTime(trip.startTime)}–{formatTime(trip.endTime)}
        </p>
        <p className="mt-0.5 mb-0 text-xs font-semibold text-ochre-foreground">
          {formatCurrency(trip.budget)}
        </p>
        <p className="mt-1 mb-0 truncate text-[0.6875rem] text-text-secondary">
          {trip.startLocationLabel}
        </p>
      </Link>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="icon-xs"
            className={cn(
              "absolute right-2 text-text-secondary",
              variant === "day-cell" ? "top-2" : "top-3",
            )}
            aria-label={`Tùy chọn cho chuyến đi ${formatTime(trip.startTime)} ngày ${trip.tripDate}`}
          >
            <MoreHorizontal aria-hidden="true" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          align="end"
          className={variant === "popover" ? "z-[1100]" : undefined}
        >
          <DropdownMenuItem destructive onSelect={() => onDelete(trip)}>
            <Trash2 aria-hidden="true" />
            Xóa chuyến đi
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

function TripDayPopover({
  year,
  month,
  day,
  trips,
  onDelete,
}: {
  year: number;
  month: number;
  day: number;
  trips: TripSummaryResponse[];
  onDelete: (trip: TripSummaryResponse) => void;
}) {
  const [open, setOpen] = useState(false);

  return (
    <div className="flex items-center justify-between gap-3">
      <p className="m-0 text-sm font-bold text-text-primary">
        {trips.length} chuyến đi
      </p>

      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="secondary"
            size="xs"
            className="gap-1.5 text-primary-strong"
            aria-label={`Xem ${trips.length} chuyến đi ngày ${day} tháng ${month}`}
          >
            <CalendarDays aria-hidden="true" />
            {trips.length}
          </Button>
        </PopoverTrigger>
        <PopoverContent
          aria-label={`Chuyến đi ngày ${day} tháng ${month} năm ${year}`}
        >
          <div className="flex items-start justify-between gap-4 border-b border-border pb-3">
            <div>
              <h2 className="m-0 text-base font-bold tracking-[-0.02em]">
                Chuyến đi ngày {String(day).padStart(2, "0")}/
                {String(month).padStart(2, "0")}
              </h2>
              <p className="mt-1 mb-0 text-xs text-text-secondary">
                Sắp xếp theo giờ bắt đầu
              </p>
            </div>
            <span className="shrink-0 rounded-full bg-primary-soft px-2 py-1 text-xs font-bold text-primary-strong">
              {trips.length}/5
            </span>
          </div>

          <div className="divide-y divide-border/80">
            {trips.map((trip) => (
              <TripEntry
                key={trip.publicId}
                trip={trip}
                onDelete={(selectedTrip) => {
                  setOpen(false);
                  onDelete(selectedTrip);
                }}
                variant="popover"
              />
            ))}
          </div>
        </PopoverContent>
      </Popover>
    </div>
  );
}

export function TripMonthBoard({
  year,
  month,
  trips,
  loading,
  onDelete,
}: TripMonthBoardProps) {
  const daysInMonth = new Date(year, month, 0).getDate();
  const tripsByDay = new Map<number, TripSummaryResponse[]>();

  for (const trip of trips) {
    const day = getTripDay(trip.tripDate);
    const dayTrips = tripsByDay.get(day) ?? [];
    dayTrips.push(trip);
    tripsByDay.set(day, dayTrips);
  }

  const today = new Date();
  const startOfToday = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate(),
  );
  const isCurrentMonth =
    today.getFullYear() === year && today.getMonth() + 1 === month;

  return (
    <section
      className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6"
      aria-label={`Bảng chuyến đi tháng ${month} năm ${year}`}
      aria-busy={loading}
    >
      {Array.from({ length: daysInMonth }, (_, index) => index + 1).map(
        (day) => {
          const dayTrips = tripsByDay.get(day) ?? [];
          const date = new Date(year, month - 1, day);
          const isToday = isCurrentMonth && today.getDate() === day;
          const isPastDate = date < startOfToday;
          const dateValue = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;

          return (
            <article
              key={day}
              className={`flex aspect-square min-h-56 flex-col overflow-hidden rounded-mint-md border bg-card p-4 shadow-mint-sm max-sm:aspect-auto ${
                isToday
                  ? "border-primary/55 ring-1 ring-primary/10"
                  : "border-border/60"
              }`}
              aria-label={formatDate(year, month, day)}
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="m-0 text-[0.6875rem] font-extrabold tracking-[0.1em] text-text-secondary uppercase">
                    {weekdayFormatter.format(date)}
                  </p>
                  <time
                    dateTime={dateValue}
                    className="mt-2 block text-5xl leading-none font-bold tracking-[-0.07em] text-primary-strong"
                  >
                    {day}
                  </time>
                  <p className="mt-1 mb-0 text-xs font-bold text-text-primary">
                    Tháng {month}
                  </p>
                </div>
                {!isPastDate ? (
                  <div className="flex shrink-0 flex-col items-end gap-2">
                    <Button
                      asChild
                      variant="ghost"
                      size="icon-sm"
                      className="rounded-full text-primary-strong hover:bg-accent-soft hover:text-accent-strong"
                    >
                      <Link
                        href={`/trips/new?date=${dateValue}`}
                        aria-label={`Tạo chuyến đi ngày ${day} tháng ${month} năm ${year}`}
                      >
                        <Plus aria-hidden="true" />
                      </Link>
                    </Button>
                    {isToday ? (
                      <span className="rounded-full bg-primary-soft px-2 py-1 text-[0.625rem] font-bold text-primary-strong">
                        Hôm nay
                      </span>
                    ) : null}
                  </div>
                ) : null}
              </div>

              <div className="mt-3 min-h-0 flex-1 border-t border-border/70 pt-3">
                {loading ? (
                  <div className="grid gap-2" aria-hidden="true">
                    <Skeleton className="h-16 rounded-lg" />
                  </div>
                ) : dayTrips.length === 1 ? (
                  <TripEntry trip={dayTrips[0]} onDelete={onDelete} />
                ) : dayTrips.length > 1 ? (
                  <TripDayPopover
                    year={year}
                    month={month}
                    day={day}
                    trips={dayTrips}
                    onDelete={onDelete}
                  />
                ) : null}
              </div>
            </article>
          );
        },
      )}
    </section>
  );
}
