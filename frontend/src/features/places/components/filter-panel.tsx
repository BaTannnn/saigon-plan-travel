"use client";

import { useId, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { FilterIcon } from "@/components/ui/icons";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { hasActiveFilters } from "@/features/places/search-params";
import { cn } from "@/lib/utils";
import type { Category } from "@/types/category";
import type { PlacesSearchFilters } from "@/types/place";

const ALL_VALUE = "__all__";

type FilterOption = {
  value: string;
  label: string;
};

type FilterSelectProps = {
  id: string;
  label: string;
  value: string;
  allLabel: string;
  options: FilterOption[];
  onValueChange: (value: string) => void;
};

function FilterSelect({
  id,
  label,
  value,
  allLabel,
  options,
  onValueChange,
}: FilterSelectProps) {
  return (
    <div className="grid gap-[5px]">
      <Label className="text-xs font-bold text-text-secondary" htmlFor={id}>
        {label}
      </Label>
      <Select value={value} onValueChange={onValueChange}>
        <SelectTrigger
          className="h-10 min-h-10 w-full min-w-0 rounded-[9px] border-border bg-background px-2.5 text-text-primary"
          id={id}
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent position="popper">
          <SelectItem value={ALL_VALUE}>{allLabel}</SelectItem>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

type FilterPanelProps = {
  categories: Category[];
  administrativeUnitNames: string[];
  filters: PlacesSearchFilters;
  className?: string;
};

export function FilterPanel({
  categories,
  administrativeUnitNames,
  filters,
  className,
}: FilterPanelProps) {
  const administrativeUnitNameId = useId();
  const categoryId = useId();
  const indoorId = useId();
  const maxCostId = useId();
  const [administrativeUnitName, setAdministrativeUnitName] = useState(
    filters.administrativeUnitName ?? ALL_VALUE,
  );
  const [category, setCategory] = useState(filters.category ?? ALL_VALUE);
  const [indoor, setIndoor] = useState(filters.indoor ?? ALL_VALUE);

  return (
    <div
      className={cn(
        "rounded-mint-md border border-border bg-surface p-3.5 shadow-mint-sm",
        className,
      )}
    >
      <div className="mb-2.5 flex items-center justify-between">
        <h2 className="m-0 flex items-center gap-[7px] text-sm font-bold">
          <FilterIcon className="size-5" /> Bộ lọc
        </h2>
      </div>

      <form
        action="/places"
        className="grid grid-cols-2 gap-2.5 max-md:grid-cols-1 md:max-[1100px]:grid-cols-1"
      >
        {filters.keyword ? (
          <input type="hidden" name="keyword" value={filters.keyword} />
        ) : null}
        <input
          type="hidden"
          name="administrativeUnitName"
          value={
            administrativeUnitName === ALL_VALUE ? "" : administrativeUnitName
          }
        />
        <input
          type="hidden"
          name="category"
          value={category === ALL_VALUE ? "" : category}
        />
        <input
          type="hidden"
          name="indoor"
          value={indoor === ALL_VALUE ? "" : indoor}
        />

        <FilterSelect
          id={administrativeUnitNameId}
          label="Đơn vị hành chính"
          value={administrativeUnitName}
          allLabel="Tất cả đơn vị hành chính"
          options={administrativeUnitNames.map((item) => ({
            value: item,
            label: item,
          }))}
          onValueChange={setAdministrativeUnitName}
        />

        <FilterSelect
          id={categoryId}
          label="Danh mục"
          value={category}
          allLabel="Tất cả danh mục"
          options={categories.map((item) => ({
            value: item.slug,
            label: item.name,
          }))}
          onValueChange={setCategory}
        />

        <FilterSelect
          id={indoorId}
          label="Không gian"
          value={indoor}
          allLabel="Trong nhà & ngoài trời"
          options={[
            { value: "true", label: "Trong nhà" },
            { value: "false", label: "Ngoài trời" },
          ]}
          onValueChange={setIndoor}
        />

        <div className="grid gap-[5px]">
          <Label
            className="text-xs font-bold text-text-secondary"
            htmlFor={maxCostId}
          >
            Chi phí tối đa (VND)
          </Label>
          <Input
            className="h-10 min-h-10 rounded-[9px] border-border bg-background px-2.5 text-text-primary"
            id={maxCostId}
            type="number"
            name="maxCost"
            min="0"
            max="100000000"
            step="10000"
            defaultValue={filters.maxCost}
            placeholder="Ví dụ: 200000"
          />
        </div>

        <Button
          className="min-h-10 self-end text-[0.8rem]"
          type="submit"
          size="sm"
        >
          Áp dụng bộ lọc
        </Button>
        {hasActiveFilters(filters) ? (
          <Button
            asChild
            className="self-center justify-self-center text-[0.78rem] font-bold underline [text-underline-offset:3px]"
            variant="link"
          >
            <Link href="/places">Xóa tất cả bộ lọc</Link>
          </Button>
        ) : null}
      </form>
    </div>
  );
}
