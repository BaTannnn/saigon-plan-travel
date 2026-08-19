import {
  ArrowDown,
  ArrowUp,
  Clock3,
  MoreHorizontal,
  PencilLine,
  Route,
  Trash2,
  WalletCards,
} from "lucide-react";
import {
  formatCurrency,
  formatDistance,
  formatTime,
} from "@/lib/formatters";
import type { ItineraryItemResponse } from "@/types/itinerary";

type ItineraryTimelineProps = {
  items: ItineraryItemResponse[];
  selectedItemPublicId: string | null;
  openMenuId: string | null;
  mutating: boolean;
  onSelectItem: (itemPublicId: string) => void;
  onToggleMenu: (itemPublicId: string | null) => void;
  onMove: (index: number, offset: -1 | 1) => Promise<void>;
  onReplace: (itemPublicId: string) => void;
  onDelete: (itemPublicId: string, placeName: string) => Promise<void>;
};

type ItineraryItemCardProps = {
  item: ItineraryItemResponse;
  index: number;
  itemCount: number;
  selected: boolean;
  menuOpen: boolean;
  mutating: boolean;
  onSelect: () => void;
  onToggleMenu: () => void;
  onMove: (offset: -1 | 1) => Promise<void>;
  onReplace: () => void;
  onDelete: () => Promise<void>;
};

function ItineraryItemCard({
  item,
  index,
  itemCount,
  selected,
  menuOpen,
  mutating,
  onSelect,
  onToggleMenu,
  onMove,
  onReplace,
  onDelete,
}: ItineraryItemCardProps) {
  return (
    <li
      className={`relative ${
        index < itemCount - 1
          ? "after:absolute after:top-10 after:bottom-[-12px] after:left-[18px] after:w-px after:bg-primary/25 after:content-['']"
          : ""
      }`}
    >
      <div
        className={`grid grid-cols-[minmax(0,1fr)_40px] items-stretch border-b py-1 transition ${
          selected
            ? "border-primary/55 bg-primary-soft/40"
            : "border-border hover:border-primary/35 hover:bg-muted/35"
        }`}
      >
        <button
          type="button"
          className="grid min-w-0 grid-cols-[36px_1fr] items-start gap-3 py-3 pl-0 text-left"
          onClick={onSelect}
        >
          <span className="z-10 grid size-9 place-items-center rounded-full bg-primary text-xs font-black text-primary-foreground">
            {String(item.sequenceNo).padStart(2, "0")}
          </span>

          <span className="min-w-0">
            <strong className="block truncate text-[0.95rem] text-text-primary">
              {item.place.name}
            </strong>
            <span className="mt-1 flex items-center gap-1 text-[0.8rem] leading-5 font-semibold text-text-primary tabular-nums">
              <Clock3
                className="size-3.5 text-primary"
                aria-hidden="true"
              />
              {formatTime(item.schedule.visitStartTime)} –{" "}
              {formatTime(item.schedule.visitEndTime)}
            </span>
            <span className="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-xs leading-5 text-text-secondary">
              <span className="tabular-nums">
                Đến {formatTime(item.schedule.arrivalTime)}
              </span>
              <span className="inline-flex items-center gap-1 tabular-nums">
                <Route className="size-3.5" aria-hidden="true" />
                {item.schedule.travelMinutes} phút ·{" "}
                {formatDistance(item.schedule.travelDistanceKm)}
              </span>
              <span className="inline-flex items-center gap-1 text-ochre-foreground tabular-nums">
                <WalletCards
                  className="size-3.5 text-ochre"
                  aria-hidden="true"
                />
                {formatCurrency(item.schedule.estimatedCost)}
              </span>
            </span>
          </span>
        </button>

        <button
          type="button"
          className="m-auto grid size-8 place-items-center rounded-md text-text-secondary transition hover:bg-muted hover:text-text-primary"
          aria-label={`Tùy chọn cho ${item.place.name}`}
          aria-expanded={menuOpen}
          disabled={mutating}
          onClick={onToggleMenu}
        >
          <MoreHorizontal className="size-5" aria-hidden="true" />
        </button>
      </div>

      {menuOpen ? (
        <div className="absolute top-11 right-1 z-20 min-w-48 rounded-lg border border-border bg-popover p-1.5 shadow-mint-md">
          <button
            type="button"
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
            onClick={() => onMove(-1)}
            disabled={mutating || index === 0}
          >
            <ArrowUp className="size-4" aria-hidden="true" />
            Di chuyển lên
          </button>
          <button
            type="button"
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
            onClick={() => onMove(1)}
            disabled={mutating || index === itemCount - 1}
          >
            <ArrowDown className="size-4" aria-hidden="true" />
            Di chuyển xuống
          </button>
          <button
            type="button"
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
            onClick={onReplace}
            disabled={mutating}
          >
            <PencilLine className="size-4" aria-hidden="true" />
            Thay địa điểm
          </button>
          <button
            type="button"
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-semibold text-destructive hover:bg-destructive/10 disabled:cursor-not-allowed disabled:opacity-45"
            onClick={onDelete}
            disabled={mutating}
          >
            <Trash2 className="size-4" aria-hidden="true" />
            Xóa khỏi hành trình
          </button>
        </div>
      ) : null}
    </li>
  );
}

export function ItineraryTimeline({
  items,
  selectedItemPublicId,
  openMenuId,
  mutating,
  onSelectItem,
  onToggleMenu,
  onMove,
  onReplace,
  onDelete,
}: ItineraryTimelineProps) {
  return (
    <ol className="grid gap-3">
      {items.map((item, index) => (
        <ItineraryItemCard
          key={item.publicId}
          item={item}
          index={index}
          itemCount={items.length}
          selected={item.publicId === selectedItemPublicId}
          menuOpen={openMenuId === item.publicId}
          mutating={mutating}
          onSelect={() => onSelectItem(item.publicId)}
          onToggleMenu={() =>
            onToggleMenu(openMenuId === item.publicId ? null : item.publicId)
          }
          onMove={(offset) => onMove(index, offset)}
          onReplace={() => onReplace(item.publicId)}
          onDelete={() => onDelete(item.publicId, item.place.name)}
        />
      ))}
    </ol>
  );
}
