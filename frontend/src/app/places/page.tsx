import type { Metadata } from "next";
import { PlacesErrorState } from "@/features/places/components/places-error-state";
import { PlacesExplorer } from "@/features/places/components/places-explorer";
import {
  readPlacesSearchParams,
  type RawSearchParams,
} from "@/features/places/search-params";
import { ApiError } from "@/lib/api/api-client";
import { getCategories } from "@/lib/api/category-api";
import { getPlaceCatalog, getPlaces } from "@/lib/api/place-api";

export const metadata: Metadata = {
  title: "Khám phá địa điểm",
};

async function loadPlacesPage(
  filters: ReturnType<typeof readPlacesSearchParams>,
) {
  try {
    const [result, categories, catalog] = await Promise.all([
      getPlaces(filters),
      getCategories(),
      getPlaceCatalog(),
    ]);

    return { result, categories, catalog, error: null };
  } catch (error) {
    if (error instanceof ApiError) {
      return { result: null, categories: null, catalog: null, error };
    }
    throw error;
  }
}

export default async function PlacesPage({
  searchParams,
}: {
  searchParams: Promise<RawSearchParams>;
}) {
  const filters = readPlacesSearchParams(await searchParams);
  const { result, categories, catalog, error } = await loadPlacesPage(filters);

  if (error) {
    return (
      <PlacesErrorState
        connectionFailure={error.status === 0 || error.status >= 500}
        detail={error.problem?.detail}
      />
    );
  }

  const administrativeUnitNames = [
    ...new Set(
      catalog.content
        .map((place) => place.administrativeUnitName)
        .filter((name): name is string => name !== null),
    ),
  ].sort((left, right) => left.localeCompare(right, "vi"));

  return (
    <PlacesExplorer
      result={result}
      categories={categories}
      administrativeUnitNames={administrativeUnitNames}
      filters={filters}
    />
  );
}
