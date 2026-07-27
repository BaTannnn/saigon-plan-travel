import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  ArrowLeftIcon,
  ClockIcon,
  PinIcon,
  WalletIcon,
} from "@/components/ui/icons";
import {
  dayNames,
  formatCost,
  formatDuration,
} from "@/features/places/formatters";
import { MapShell } from "@/features/map/map-shell";
import { cn } from "@/lib/utils";
import type { PlaceDetail } from "@/types/place";

type PlaceDetailViewProps = {
  place: PlaceDetail;
};

const eyebrowClassName =
  "m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase";

const detailCardClassName =
  "block rounded-mint-md border border-border bg-surface p-[clamp(22px,4vw,36px)] shadow-mint-sm ring-0";

const quickFactClassName =
  "min-h-[42px] rounded-xl border-border bg-background px-3 text-xs font-bold text-text-primary";

export function PlaceDetailView({ place }: PlaceDetailViewProps) {
  const hoursByDay = new Map(
    place.openingHours.map((item) => [item.dayOfWeek, item]),
  );

  return (
    <main className="mx-auto w-[min(1180px,calc(100%_-_40px))] pt-7 pb-16 max-md:w-[min(calc(100%_-_28px),1180px)] max-md:pt-[18px]">
      <Button
        asChild
        className="mb-[18px] min-h-11 p-0 font-extrabold"
        variant="link"
      >
        <Link href="/places">
          <ArrowLeftIcon /> Quay lại khám phá
        </Link>
      </Button>

      <Card asChild>
        <section className="grid grid-cols-[minmax(280px,0.85fr)_1.25fr] items-center gap-[clamp(28px,6vw,72px)] rounded-mint-lg border border-border bg-surface p-[clamp(24px,5vw,56px)] shadow-mint-sm ring-0 max-md:grid-cols-1 max-md:gap-6 max-md:p-4">
          <div
            className="relative grid min-h-[310px] place-items-center overflow-hidden rounded-mint-lg bg-[linear-gradient(145deg,var(--primary-soft),color-mix(in_srgb,var(--accent-soft)_60%,var(--surface)))] text-primary-strong max-md:min-h-[220px]"
            aria-hidden="true"
          >
            <span className="relative z-2 grid size-28 place-items-center rounded-[50%_50%_50%_24px] border border-surface bg-white/60 text-[3.2rem] font-black backdrop-blur-sm">
              {place.categories[0]?.name.slice(0, 1) ?? "S"}
            </span>
            <div className="absolute size-[280px] rounded-full border-2 border-dashed border-primary/20" />
            <p className="absolute inset-x-5 bottom-4 m-0 text-center font-extrabold">
              {place.categories[0]?.name ?? "Khám phá"}
            </p>
          </div>
          <div className="max-md:px-1.5 max-md:pt-0.5 max-md:pb-3">
            <div className="mb-[18px] flex flex-wrap items-center gap-[7px]">
              {place.categories.map((category) => (
                <Badge
                  className="h-10 rounded-full border-[color-mix(in_srgb,var(--primary)_42%,var(--border))] bg-primary-soft px-3.5 text-[0.82rem] font-bold text-primary-strong"
                  key={category.id}
                >
                  {category.name}
                </Badge>
              ))}
            </div>
            <p className={eyebrowClassName}>
              Đơn vị hành chính:{" "}
              {place.administrativeUnitName ?? "Chưa xác định"}
            </p>
            <h1 className="mt-2 mb-3.5 text-[clamp(2.2rem,5vw,4.5rem)] leading-[1.02] font-bold tracking-[-0.055em] max-md:text-[2.4rem]">
              {place.name}
            </h1>
            <p className="m-0 max-w-[700px] text-[1.05rem] leading-[1.7] text-text-secondary">
              {place.shortDescription ?? "Chưa có mô tả ngắn cho địa điểm này."}
            </p>
            <div className="mt-6 flex flex-wrap gap-2.5">
              <Badge className={quickFactClassName} variant="outline">
                <ClockIcon /> {formatDuration(place.estimatedVisitMinutes)}
              </Badge>
              <Badge className={quickFactClassName} variant="outline">
                <WalletIcon /> {formatCost(place.minCost, place.maxCost)}
              </Badge>
              <Badge className={quickFactClassName} variant="outline">
                {place.indoor ? "Trong nhà" : "Ngoài trời"}
              </Badge>
            </div>
          </div>
        </section>
      </Card>

      <div className="mt-6 grid grid-cols-[1.25fr_0.75fr] items-start gap-6 max-md:grid-cols-1">
        <div className="grid gap-6">
          <Card asChild>
            <section className={detailCardClassName}>
              <p className={eyebrowClassName}>Giới thiệu</p>
              <h2 className="mt-[5px] mb-3.5 text-2xl font-bold">
                Về địa điểm
              </h2>
              <p className="m-0 leading-[1.8] text-text-secondary">
                {place.fullDescription ??
                  place.shortDescription ??
                  "Chưa có mô tả chi tiết."}
              </p>
            </section>
          </Card>

          <Card asChild>
            <section className={detailCardClassName}>
              <p className={eyebrowClassName}>Lịch hoạt động</p>
              <h2 className="mt-[5px] mb-3.5 text-2xl font-bold">Giờ mở cửa</h2>
              <div className="grid">
                {Object.entries(dayNames).map(([day, label]) => {
                  const opening = hoursByDay.get(Number(day));
                  return (
                    <div
                      className="flex justify-between gap-5 border-b border-border py-[13px] last:border-b-0 max-md:text-sm"
                      key={day}
                    >
                      <span>{label}</span>
                      <strong
                        className={cn(
                          "text-right text-primary",
                          opening?.closed && "text-accent",
                        )}
                      >
                        {!opening
                          ? "Chưa có dữ liệu"
                          : opening.closed
                            ? "Đóng cửa"
                            : `${opening.openTime} – ${opening.closeTime}`}
                      </strong>
                    </div>
                  );
                })}
              </div>
            </section>
          </Card>
        </div>

        <aside className="sticky top-[100px] max-md:static max-md:row-start-1">
          <Card asChild>
            <section className="block rounded-mint-md border border-border bg-surface p-3 shadow-mint-sm ring-0">
              <div className="h-[330px] overflow-hidden rounded-[13px] max-md:h-[42vh] max-md:min-h-[300px]">
                <MapShell
                  places={[place]}
                  selectedSlug={place.slug}
                  detailMode
                />
              </div>
              <div className="flex items-start gap-3 px-2 pt-[18px] pb-2">
                <PinIcon className="mt-0.5 size-5 shrink-0 text-primary" />
                <div>
                  <strong>Địa chỉ</strong>
                  <p className="mt-1 mb-0 leading-[1.55] text-text-secondary">
                    {place.address}
                    {place.administrativeUnitName
                      ? `, ${place.administrativeUnitName}`
                      : ""}
                  </p>
                </div>
              </div>
              <p className="mx-2 mt-[5px] mb-2 text-xs text-text-secondary">
                {place.latitude.toFixed(5)}, {place.longitude.toFixed(5)}
              </p>
            </section>
          </Card>
        </aside>
      </div>
    </main>
  );
}
