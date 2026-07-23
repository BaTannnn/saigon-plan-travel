import { SearchIcon } from "@/components/ui/icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import type { PlacesSearchFilters } from "@/types/place";

type SearchFormProps = {
  filters: PlacesSearchFilters;
  compact?: boolean;
};

export function SearchForm({ filters, compact = false }: SearchFormProps) {
  return (
    <form
      className={cn(
        "flex min-h-[52px] items-center gap-2 rounded-[15px] border border-border bg-surface py-[5px] pr-1.5 pl-4 shadow-mint-md max-md:min-h-[50px] max-md:shadow-mint-sm",
        compact && "w-full",
      )}
      action="/places"
      role="search"
    >
      <SearchIcon className="size-5 shrink-0 text-primary-strong" />
      <Label className="sr-only" htmlFor={compact ? "map-keyword" : "keyword"}>
        Tìm địa điểm
      </Label>
      <Input
        className="h-[42px] min-w-0 flex-1 border-0 bg-transparent px-0 text-text-primary shadow-none outline-none focus-visible:border-0 focus-visible:ring-0"
        id={compact ? "map-keyword" : "keyword"}
        name="keyword"
        type="search"
        defaultValue={filters.keyword}
        placeholder="Tìm địa điểm"
        maxLength={100}
      />
      {filters.district ? (
        <input type="hidden" name="district" value={filters.district} />
      ) : null}
      {filters.category ? (
        <input type="hidden" name="category" value={filters.category} />
      ) : null}
      {filters.indoor ? (
        <input type="hidden" name="indoor" value={filters.indoor} />
      ) : null}
      {filters.maxCost ? (
        <input type="hidden" name="maxCost" value={filters.maxCost} />
      ) : null}
      <Button
        className="min-h-[42px] min-w-[66px] rounded-[10px] font-extrabold"
        type="submit"
        size="sm"
      >
        Tìm
      </Button>
    </form>
  );
}
