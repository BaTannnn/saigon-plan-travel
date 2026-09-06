"use client";

import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ChevronLeft, ChevronRight } from "lucide-react";

type TripMonthToolbarProps = {
  month: number;
  year: number;
  years: number[];
  previousDisabled: boolean;
  nextDisabled: boolean;
  onMonthChange: (month: number) => void;
  onYearChange: (year: number) => void;
  onPreviousMonth: () => void;
  onNextMonth: () => void;
};

export function TripMonthToolbar({
  month,
  year,
  years,
  previousDisabled,
  nextDisabled,
  onMonthChange,
  onYearChange,
  onPreviousMonth,
  onNextMonth,
}: TripMonthToolbarProps) {
  return (
    <div
      className="inline-flex h-11 w-fit max-w-full items-center rounded-full border border-border/80 bg-card px-1 shadow-mint-sm"
      role="group"
      aria-label="Chọn tháng xem chuyến đi"
    >
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        className="rounded-full text-text-secondary hover:bg-primary-soft/60 hover:text-primary-strong"
        disabled={previousDisabled}
        onClick={onPreviousMonth}
        aria-label="Xem tháng trước"
      >
        <ChevronLeft aria-hidden="true" />
      </Button>

      <Select
        value={String(month)}
        onValueChange={(value) => onMonthChange(Number(value))}
      >
        <SelectTrigger
          className="h-10 w-24 justify-center gap-2 rounded-none border-0 bg-transparent px-2 font-semibold text-text-primary shadow-none focus-visible:border-transparent focus-visible:ring-2 focus-visible:ring-ring/35"
          aria-label="Tháng"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {Array.from({ length: 12 }, (_, index) => index + 1).map(
            (monthOption) => (
              <SelectItem key={monthOption} value={String(monthOption)}>
                Tháng {monthOption}
              </SelectItem>
            ),
          )}
        </SelectContent>
      </Select>

      <span className="h-6 w-px shrink-0 bg-border" aria-hidden="true" />

      <Select
        value={String(year)}
        onValueChange={(value) => onYearChange(Number(value))}
      >
        <SelectTrigger
          className="h-10 w-20 justify-center gap-2 rounded-none border-0 bg-transparent px-2 font-semibold text-text-primary shadow-none focus-visible:border-transparent focus-visible:ring-2 focus-visible:ring-ring/35"
          aria-label="Năm"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {years.map((yearOption) => (
            <SelectItem key={yearOption} value={String(yearOption)}>
              {yearOption}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        className="rounded-full text-text-secondary hover:bg-primary-soft/60 hover:text-primary-strong"
        disabled={nextDisabled}
        onClick={onNextMonth}
        aria-label="Xem tháng sau"
      >
        <ChevronRight aria-hidden="true" />
      </Button>
    </div>
  );
}
