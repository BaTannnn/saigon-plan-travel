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
      className="flex flex-wrap items-center gap-2"
      role="group"
      aria-label="Chọn tháng xem chuyến đi"
    >
      <Button
        type="button"
        variant="outline"
        size="icon-sm"
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
        <SelectTrigger className="h-10 min-w-28 bg-card" aria-label="Tháng">
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

      <Select
        value={String(year)}
        onValueChange={(value) => onYearChange(Number(value))}
      >
        <SelectTrigger className="h-10 min-w-24 bg-card" aria-label="Năm">
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
        variant="outline"
        size="icon-sm"
        disabled={nextDisabled}
        onClick={onNextMonth}
        aria-label="Xem tháng sau"
      >
        <ChevronRight aria-hidden="true" />
      </Button>
    </div>
  );
}
