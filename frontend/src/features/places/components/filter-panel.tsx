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
import type { Category, PlacesSearchFilters } from "@/types/place";

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
    <div className="filter-field">
      <Label htmlFor={id}>{label}</Label>
      <Select value={value} onValueChange={onValueChange}>
        <SelectTrigger className="filter-select-trigger" id={id}>
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
  districts: string[];
  filters: PlacesSearchFilters;
};

export function FilterPanel({
  categories,
  districts,
  filters,
}: FilterPanelProps) {
  const districtId = useId();
  const categoryId = useId();
  const indoorId = useId();
  const maxCostId = useId();
  const [district, setDistrict] = useState(filters.district ?? ALL_VALUE);
  const [category, setCategory] = useState(filters.category ?? ALL_VALUE);
  const [indoor, setIndoor] = useState(filters.indoor ?? ALL_VALUE);

  return (
    <div className="filter-panel">
      <div className="filter-panel-heading">
        <h2>
          <FilterIcon /> Bộ lọc
        </h2>
      </div>

      <form action="/places" className="filter-form">
        {filters.keyword ? (
          <input type="hidden" name="keyword" value={filters.keyword} />
        ) : null}
        <input
          type="hidden"
          name="district"
          value={district === ALL_VALUE ? "" : district}
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
          id={districtId}
          label="Quận / khu vực"
          value={district}
          allLabel="Tất cả khu vực"
          options={districts.map((item) => ({ value: item, label: item }))}
          onValueChange={setDistrict}
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

        <div className="filter-field">
          <Label htmlFor={maxCostId}>Chi phí tối đa (VND)</Label>
          <Input
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

        <Button className="filter-submit-button" type="submit" size="sm">
          Áp dụng bộ lọc
        </Button>
        {hasActiveFilters(filters) ? (
          <Button asChild className="clear-filter-link" variant="link">
            <Link href="/places">Xóa tất cả bộ lọc</Link>
          </Button>
        ) : null}
      </form>
    </div>
  );
}
