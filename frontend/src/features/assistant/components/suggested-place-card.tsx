import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Check, MapPin, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { AssistantSuggestedPlace } from "@/types/assistant";

type AddState = "idle" | "pending" | "success" | "error";

type SuggestedPlaceCardProps = {
  place: AssistantSuggestedPlace;
  alreadyInItinerary: boolean;
  onAddPlace: (placeId: number) => Promise<void>;
};

export function SuggestedPlaceCard({
  place,
  alreadyInItinerary,
  onAddPlace,
}: SuggestedPlaceCardProps) {
  const [addState, setAddState] = useState<AddState>("idle");

  async function handleAdd() {
    if (alreadyInItinerary || addState === "pending") return;

    setAddState("pending");
    try {
      await onAddPlace(place.placeId);
      setAddState("success");
    } catch {
      setAddState("error");
    }
  }

  const added = alreadyInItinerary || addState === "success";

  return (
    <article className="overflow-hidden rounded-mint-md border border-border bg-surface">
      {place.primaryImageUrl ? (
        <Image
          className="h-28 w-full object-cover"
          src={place.primaryImageUrl}
          alt=""
          width={360}
          height={112}
          loading="lazy"
        />
      ) : null}

      <div className="p-3.5">
        <div className="flex items-start gap-2">
          {!place.primaryImageUrl ? (
            <span className="mt-0.5 grid size-7 shrink-0 place-items-center rounded-full bg-primary-soft text-primary">
              <MapPin className="size-3.5" aria-hidden="true" />
            </span>
          ) : null}
          <div className="min-w-0">
            <h4 className="m-0 text-sm font-bold wrap-break-word text-text-primary">
              {place.name}
            </h4>
            <p className="mt-1 mb-0 line-clamp-3 text-xs leading-5 text-text-secondary">
              {place.reason}
            </p>
          </div>
        </div>

        <div className="mt-3 flex flex-wrap gap-2">
          <Button asChild size="xs" variant="outline">
            <Link href={`/places/${encodeURIComponent(place.slug)}`}>
              Xem địa điểm
            </Link>
          </Button>
          <Button
            type="button"
            size="xs"
            variant={added ? "secondary" : "accent"}
            disabled={added || addState === "pending"}
            onClick={() => void handleAdd()}
          >
            {added ? (
              <Check className="size-3" aria-hidden="true" />
            ) : (
              <Plus className="size-3" aria-hidden="true" />
            )}
            {alreadyInItinerary
              ? "Đã có trong hành trình"
              : addState === "pending"
                ? "Đang thêm..."
                : addState === "success"
                  ? "Đã thêm"
                  : addState === "error"
                    ? "Thử thêm lại"
                    : "Thêm vào hành trình"}
          </Button>
        </div>

        {addState === "error" && !alreadyInItinerary ? (
          <p className="mt-2 mb-0 text-xs text-destructive" role="alert">
            Không thể thêm địa điểm. Bạn có thể thử lại.
          </p>
        ) : null}
      </div>
    </article>
  );
}
