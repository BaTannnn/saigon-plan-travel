import { SearchIcon } from "@/components/ui/icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { PlacesSearchFilters } from "@/types/place";

type SearchFormProps = {
  filters: PlacesSearchFilters;
  compact?: boolean;
};

export function SearchForm({ filters, compact = false }: SearchFormProps) {
  return (
    <form
      className={compact ? "search-form search-form-compact" : "search-form"}
      action="/places"
      role="search"
    >
      <SearchIcon className="search-form-icon" />
      <Label className="sr-only" htmlFor={compact ? "map-keyword" : "keyword"}>
        Tìm địa điểm
      </Label>
      <Input
        className="search-input"
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
      <Button className="search-submit-button" type="submit" size="sm">
        Tìm
      </Button>
    </form>
  );
}
