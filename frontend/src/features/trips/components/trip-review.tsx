import { MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import type {
  EnvironmentPreference,
  TravelPace,
  TripResponse,
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
    day: "numeric",
    month: "long",
  }).format(new Date(`${value}T00:00:00`));
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDuration(startTime: string, endTime: string) {
  const [startHour, startMinute] = startTime.split(":").map(Number);
  const [endHour, endMinute] = endTime.split(":").map(Number);
  const totalMinutes =
    endHour * 60 + endMinute - (startHour * 60 + startMinute);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  return minutes === 0
    ? `${hours} giờ`
    : `${hours} giờ ${minutes} phút`;
}

function OverviewStat({
  label,
  value,
  className,
}: {
  label: string;
  value: string;
  className: string;
}) {
  return (
    <div className={className}>
      <p className="m-0 break-words text-[1.35rem] leading-tight font-bold tracking-[-0.025em] text-text-primary">
        {value}
      </p>
      <p className="mt-2 mb-0 text-sm font-medium text-text-secondary">
        {label}
      </p>
    </div>
  );
}

export function TripReview({
  trip,
  onEdit,
}: {
  trip: TripResponse;
  onEdit: () => void;
}) {
  const duration = formatDuration(trip.startTime, trip.endTime);

  return (
    <article>
      <header>
        <h1 className="m-0 max-w-2xl text-[clamp(2.65rem,5vw,3.75rem)] leading-[1.02] font-bold tracking-[-0.055em] capitalize">
          {formatDate(trip.tripDate)}
        </h1>

        <div
          className="mt-8 border-y border-border py-5"
          aria-label={`Từ ${trip.startTime}, trong ${duration}, đến ${trip.endTime}`}
        >
          <div className="grid grid-cols-3 items-end gap-3">
            <time
              className="text-2xl font-bold tracking-[-0.025em] text-primary-strong tabular-nums"
              dateTime={trip.startTime}
            >
              {trip.startTime}
            </time>
            <span className="text-center text-lg font-bold tracking-[-0.02em] whitespace-nowrap text-text-primary">
              {duration}
            </span>
            <time
              className="text-right text-2xl font-bold tracking-[-0.025em] text-primary-strong tabular-nums"
              dateTime={trip.endTime}
            >
              {trip.endTime}
            </time>
          </div>
          <div className="mt-4 h-2.5 w-full rounded-full bg-primary" aria-hidden="true" />
        </div>
      </header>

      <section
        className="grid grid-cols-[28px_minmax(0,1fr)] gap-3 border-b border-border py-7"
        aria-labelledby="trip-origin-heading"
      >
        <MapPin className="mt-1 size-5 text-primary" aria-hidden="true" />
        <div className="min-w-0">
          <p className="m-0 break-words text-xl leading-snug font-bold tracking-[-0.025em]">
            {trip.startLocation.label}
          </p>
          <h2
            id="trip-origin-heading"
            className="mt-1 mb-0 text-sm font-medium text-text-secondary"
          >
            Điểm xuất phát
          </h2>
        </div>
      </section>

      <section className="grid grid-cols-1 border-b border-border sm:grid-cols-3" aria-label="Thông tin chuyến đi">
        <OverviewStat
          className="py-6 sm:pr-6"
          label="Ngân sách"
          value={formatMoney(trip.budget)}
        />
        <OverviewStat
          className="border-t border-border py-6 sm:border-t-0 sm:border-l sm:px-6"
          label="Nhịp độ"
          value={paceLabels[trip.travelPace]}
        />
        <OverviewStat
          className="border-t border-border py-6 sm:border-t-0 sm:border-l sm:pl-6"
          label="Không gian"
          value={environmentLabels[trip.environmentPreference]}
        />
      </section>

      <footer className="mt-6 flex flex-wrap items-end justify-between gap-4">
        <p className="m-0 text-xs text-text-secondary">
          Cập nhật lần cuối:{" "}
          {new Intl.DateTimeFormat("vi-VN", {
            dateStyle: "medium",
            timeStyle: "short",
          }).format(new Date(trip.updatedAt))}
        </p>
        <Button type="button" size="sm" variant="outline" onClick={onEdit}>
          Chỉnh sửa
        </Button>
      </footer>
    </article>
  );
}
