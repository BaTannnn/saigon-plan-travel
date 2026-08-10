import { Badge } from "@/components/ui/badge";
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
}: {
  label: string;
  value: string;
}) {
  return (
    <div>
      <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-text-secondary uppercase">
        {label}
      </p>
      <p className="mt-2 mb-0 text-xl font-bold tracking-[-0.025em] text-text-primary">
        {value}
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
        <p className="m-0 text-sm font-extrabold tracking-[0.14em] text-primary uppercase">
          TP.HCM
        </p>
        <h1 className="mt-2 mb-0 text-[clamp(2.2rem,5vw,3.7rem)] leading-[1.05] font-bold tracking-[-0.055em] capitalize">
          {formatDate(trip.tripDate)}
        </h1>

        <div
          className="mt-7 grid grid-cols-[auto_minmax(40px,1fr)_auto] items-center gap-4 text-lg font-bold text-primary-strong"
          aria-label={`Từ ${trip.startTime} đến ${trip.endTime}`}
        >
          <time dateTime={trip.startTime}>{trip.startTime}</time>
          <span className="flex items-center" aria-hidden="true">
            <span className="size-2 rounded-full bg-primary" />
            <span className="h-px flex-1 bg-primary/40" />
            <span className="size-2 rounded-full bg-primary" />
          </span>
          <time dateTime={trip.endTime}>{trip.endTime}</time>
        </div>
        <p className="mt-2 mb-0 text-center text-sm font-semibold text-text-secondary">
          Thời lượng {duration}
        </p>
      </header>

      <section className="mt-10" aria-labelledby="trip-origin-heading">
        <h2
          id="trip-origin-heading"
          className="m-0 text-xs font-extrabold tracking-[0.12em] text-text-secondary uppercase"
        >
          Điểm xuất phát
        </h2>
        <div className="mt-3 grid grid-cols-[12px_1fr] items-start gap-3">
          <span
            className="mt-2 size-2 rounded-full bg-primary"
            aria-hidden="true"
          />
          <div>
            <p className="m-0 text-xl font-bold">
              {trip.startLocation.label}
            </p>
            <p className="mt-1.5 mb-0 text-[0.72rem] tracking-[0.02em] text-text-secondary/70">
              Tọa độ: {" "}
              {trip.startLocation.latitude.toFixed(7)}, {" "}
              {trip.startLocation.longitude.toFixed(7)}
            </p>
          </div>
        </div>
      </section>

      <section
        className="mt-10 border-t border-border pt-8"
        aria-label="Thông tin chuyến đi"
      >
        <div className="grid grid-cols-2 gap-x-12 gap-y-8 max-sm:grid-cols-1">
          <OverviewStat label="Ngân sách" value={formatMoney(trip.budget)} />
          <OverviewStat
            label="Nhịp độ"
            value={paceLabels[trip.travelPace]}
          />
          <OverviewStat
            label="Không gian"
            value={environmentLabels[trip.environmentPreference]}
          />
        </div>
      </section>

      <section
        className="mt-10 border-t border-border pt-8"
        aria-labelledby="trip-preferences-heading"
      >
        <h2
          id="trip-preferences-heading"
          className="m-0 text-xs font-extrabold tracking-[0.12em] text-text-secondary uppercase"
        >
          Sở thích
        </h2>
        <div className="mt-4 flex flex-wrap gap-2">
          {trip.categoryPreferences.map((category) => (
            <Badge
              key={category.id}
              className="h-8 rounded-full bg-primary-soft px-3 text-primary-strong"
              variant="secondary"
            >
              {category.name}
            </Badge>
          ))}
        </div>
      </section>

      <footer className="mt-10 border-t border-border pt-8">
        <Button type="button" variant="outline" onClick={onEdit}>
          Chỉnh sửa
        </Button>
        <p className="mt-5 mb-0 text-xs text-text-secondary">
          Cập nhật lần cuối:{" "}
          {new Intl.DateTimeFormat("vi-VN", {
            dateStyle: "medium",
            timeStyle: "short",
          }).format(new Date(trip.updatedAt))}
        </p>
      </footer>
    </article>
  );
}
