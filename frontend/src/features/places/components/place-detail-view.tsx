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
import { dayNames, formatCost, formatDuration } from "@/features/places/formatters";
import { MapShell } from "@/features/map/map-shell";
import type { PlaceDetail } from "@/types/place";

type PlaceDetailViewProps = {
  place: PlaceDetail;
};

export function PlaceDetailView({ place }: PlaceDetailViewProps) {
  const hoursByDay = new Map(place.openingHours.map((item) => [item.dayOfWeek, item]));

  return (
    <main className="place-detail-page">
      <Button asChild className="back-link" variant="link">
        <Link href="/places">
          <ArrowLeftIcon /> Quay lại khám phá
        </Link>
      </Button>

      <Card asChild>
        <section className="detail-hero">
          <div className="detail-illustration" aria-hidden="true">
            <span>{place.categories[0]?.name.slice(0, 1) ?? "S"}</span>
            <div />
            <p>{place.categories[0]?.name ?? "Khám phá"}</p>
          </div>
          <div className="detail-heading">
            <div className="detail-category-row">
              {place.categories.map((category) => (
                <Badge className="category-chip active" key={category.id}>
                  {category.name}
                </Badge>
              ))}
            </div>
            <p className="eyebrow">Địa điểm tại {place.district}</p>
            <h1>{place.name}</h1>
            <p className="detail-lead">
              {place.shortDescription ?? "Chưa có mô tả ngắn cho địa điểm này."}
            </p>
            <div className="detail-quick-facts">
              <Badge variant="outline"><ClockIcon /> {formatDuration(place.estimatedVisitMinutes)}</Badge>
              <Badge variant="outline"><WalletIcon /> {formatCost(place.minCost, place.maxCost)}</Badge>
              <Badge variant="outline">{place.indoor ? "Trong nhà" : "Ngoài trời"}</Badge>
            </div>
          </div>
        </section>
      </Card>

      <div className="detail-grid">
        <div className="detail-main-column">
          <Card asChild>
            <section className="detail-section">
              <p className="eyebrow">Giới thiệu</p>
              <h2>Về địa điểm</h2>
              <p>{place.fullDescription ?? place.shortDescription ?? "Chưa có mô tả chi tiết."}</p>
            </section>
          </Card>

          <Card asChild>
            <section className="detail-section">
              <p className="eyebrow">Lịch hoạt động</p>
              <h2>Giờ mở cửa</h2>
              <div className="opening-hours-list">
                {Object.entries(dayNames).map(([day, label]) => {
                  const opening = hoursByDay.get(Number(day));
                  return (
                    <div className="opening-hour-row" key={day}>
                      <span>{label}</span>
                      <strong className={opening?.closed ? "closed" : ""}>
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

        <aside className="detail-side-column">
          <Card asChild>
            <section className="detail-location-card">
              <div className="detail-map-wrapper">
                <MapShell places={[place]} selectedSlug={place.slug} detailMode />
              </div>
              <div className="address-block">
                <PinIcon />
                <div><strong>Địa chỉ</strong><p>{place.address}, {place.district}</p></div>
              </div>
              <p className="coordinate-note">
                {place.latitude.toFixed(5)}, {place.longitude.toFixed(5)}
              </p>
            </section>
          </Card>
        </aside>
      </div>
    </main>
  );
}
