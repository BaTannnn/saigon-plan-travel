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
import { cn } from "@/lib/utils";
import type { PlaceSummary } from "@/types/place";

type PlaceCardProps = {
  place: PlaceSummary;
  selected: boolean;
  index: number;
  onSelect: (slug: string) => void;
};

export function PlaceCard({
  place,
  selected,
  index,
  onSelect,
}: PlaceCardProps) {
  return (
    <Card asChild>
      <article
        className={cn(
          "relative flex shrink-0 flex-row gap-3 overflow-visible rounded-mint-md border border-border/50 bg-card p-4 shadow-mint-sm ring-0 transition-[border-color,box-shadow] duration-150 hover:border-primary/35 max-md:w-full max-md:p-3.5",
          selected && "border-primary/50",
        )}
        id={`place-card-${place.slug}`}
      >
        <div
          className={cn(
            "grid size-[34px] shrink-0 place-items-center rounded-[12px_12px_12px_4px] bg-primary text-[0.82rem] font-extrabold text-surface max-md:size-[30px]",
            selected && "bg-primary-strong",
          )}
          aria-hidden="true"
        >
          {index + 1}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <div>
              <Badge
                className="mb-[3px] text-[0.68rem] font-extrabold text-primary"
                variant="secondary"
              >
                {place.indoor ? "Trong nhà" : "Ngoài trời"}
              </Badge>
              <h3 className="m-0 text-base leading-[1.28] font-bold">
                {place.name}
              </h3>
            </div>
            <Button
              type="button"
              className={cn(
                "size-10 shrink-0 rounded-full border-0 bg-primary-soft text-primary",
                selected &&
                  "bg-primary text-primary-foreground hover:bg-primary-strong",
              )}
              onClick={() => onSelect(place.slug)}
              aria-pressed={selected}
              aria-label={`Chọn ${place.name} trên bản đồ`}
              size="icon"
              variant="secondary"
            >
              <PinIcon />
            </Button>
          </div>

          <p className="my-2 line-clamp-2 text-[0.8rem] leading-6 text-text-secondary">
            {place.shortDescription ?? "Chưa có mô tả ngắn cho địa điểm này."}
          </p>
          <div className="flex flex-wrap gap-x-3 gap-y-[7px]">
            <span className="inline-flex items-center gap-[5px] text-xs font-bold text-text-secondary">
              <ClockIcon className="size-3.5 text-primary" />{" "}
              {formatDuration(place.estimatedVisitMinutes)}
            </span>
            <span className="inline-flex items-center gap-[5px] text-xs font-bold text-text-secondary">
              <WalletIcon className="size-3.5 text-primary" />{" "}
              {formatCost(place.minCost, place.maxCost)}
            </span>
          </div>
          <Button
            asChild
            className="mt-[7px] h-auto min-h-9 p-0 text-[0.8rem] font-extrabold"
            variant="link"
          >
            <Link href={`/places/${place.slug}`}>
              Xem chi tiết <ChevronRightIcon className="size-4" />
            </Link>
          </Button>
        </div>
      </article>
    </Card>
  );
}
