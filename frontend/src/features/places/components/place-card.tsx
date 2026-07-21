import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  ChevronRightIcon,
  ClockIcon,
  PinIcon,
  WalletIcon,
} from "@/components/ui/icons";
import { formatCost, formatDuration } from "@/features/places/formatters";
import type { PlaceSummary } from "@/types/place";

type PlaceCardProps = {
  place: PlaceSummary;
  selected: boolean;
  index: number;
  onSelect: (slug: string) => void;
};

export function PlaceCard({ place, selected, index, onSelect }: PlaceCardProps) {
  return (
    <Card asChild>
      <article
        className={selected ? "place-card selected" : "place-card"}
        id={`place-card-${place.slug}`}
      >
        <div className="place-card-accent" aria-hidden="true">
          {index + 1}
        </div>
        <div className="place-card-content">
          <div className="place-card-title-row">
            <div>
              <Badge className="place-kind" variant="secondary">
                {place.indoor ? "Trong nhà" : "Ngoài trời"}
              </Badge>
              <h3>{place.name}</h3>
            </div>
            <Button
              type="button"
              className="map-select-button"
              onClick={() => onSelect(place.slug)}
              aria-pressed={selected}
              aria-label={`Chọn ${place.name} trên bản đồ`}
              size="icon"
              variant="secondary"
            >
              <PinIcon />
            </Button>
          </div>

          <p className="place-description">
            {place.shortDescription ?? "Chưa có mô tả ngắn cho địa điểm này."}
          </p>
          <div className="place-card-meta">
            <span><PinIcon /> {place.district}</span>
            <span><ClockIcon /> {formatDuration(place.estimatedVisitMinutes)}</span>
            <span><WalletIcon /> {formatCost(place.minCost, place.maxCost)}</span>
          </div>
          <Button asChild className="place-detail-link" variant="link">
            <Link href={`/places/${place.slug}`}>
              Xem chi tiết <ChevronRightIcon />
            </Link>
          </Button>
        </div>
      </article>
    </Card>
  );
}
