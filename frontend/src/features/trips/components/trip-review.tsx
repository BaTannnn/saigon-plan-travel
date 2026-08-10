import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import type { EnvironmentPreference, TravelPace, TripResponse } from "@/types/trip";

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

export function TripReview({ trip, onEdit }: { trip: TripResponse; onEdit: () => void }) {
  const factClassName = "rounded-mint-md border-border bg-surface p-5 ring-0";

  return (
    <div className="grid gap-5">
      <Card className="rounded-mint-lg border-border bg-[linear-gradient(145deg,var(--surface),var(--primary-soft))] p-7 shadow-mint-sm ring-0 max-md:p-5">
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
          Chuyến đi đã lưu
        </p>
        <h1 className="mt-2 mb-2 text-[clamp(2rem,5vw,3.3rem)] leading-[1.08] font-bold tracking-[-0.05em]">
          {formatDate(trip.tripDate)}
        </h1>
        <p className="m-0 text-lg font-bold text-primary-strong">
          {trip.startTime} – {trip.endTime}
        </p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Button type="button" onClick={onEdit}>Chỉnh sửa sở thích</Button>
          <Button type="button" variant="accent" disabled>
            Tạo lịch trình · FE-F03
          </Button>
        </div>
      </Card>

      <div className="grid grid-cols-2 gap-5 max-md:grid-cols-1">
        <Card className={factClassName}>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">Ngân sách</p>
          <p className="mt-2 mb-0 text-2xl font-bold">{formatMoney(trip.budget)}</p>
          <p className="mt-1 mb-0 text-sm text-text-secondary">Ước tính cho một người</p>
        </Card>

        <Card className={factClassName}>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">Phong cách</p>
          <p className="mt-2 mb-0 text-xl font-bold">{paceLabels[trip.travelPace]}</p>
          <p className="mt-1 mb-0 text-sm text-text-secondary">{environmentLabels[trip.environmentPreference]}</p>
        </Card>
      </div>

      <Card className={factClassName}>
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">Điểm xuất phát</p>
        <h2 className="mt-2 mb-1 text-xl font-bold">{trip.startLocation.label}</h2>
        <p className="m-0 text-sm text-text-secondary">
          {trip.startLocation.latitude.toFixed(7)}, {trip.startLocation.longitude.toFixed(7)}
        </p>
      </Card>

      <Card className={factClassName}>
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">Danh mục yêu thích</p>
        <div className="mt-3 flex flex-wrap gap-2">
          {trip.categoryPreferences.map((category) => (
            <Badge key={category.id} className="h-9 rounded-full bg-primary-soft px-3 text-primary-strong" variant="secondary">
              {category.name}
            </Badge>
          ))}
        </div>
      </Card>

      <p className="m-0 text-xs text-text-secondary">
        Cập nhật lần cuối: {new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(trip.updatedAt))}
      </p>
    </div>
  );
}
