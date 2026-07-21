import type { Metadata } from "next";
import { PlacesErrorState } from "@/features/places/components/places-error-state";
import { PlacesExplorer } from "@/features/places/components/places-explorer";
import { readPlacesSearchParams, type RawSearchParams } from "@/features/places/search-params";
import {
  getCategories,
  getPlaceCatalog,
  getPlaces,
  PlaceApiError,
} from "@/lib/api/place-api";

export const metadata: Metadata = {
  title: "Khám phá địa điểm",
};

async function loadPlacesPage(filters: ReturnType<typeof readPlacesSearchParams>) {
  try {
    const [result, categories, catalog] = await Promise.all([
      getPlaces(filters),
      getCategories(),
      getPlaceCatalog(),
    ]);

    return { result, categories, catalog, error: null };
  } catch (error) {
    if (error instanceof PlaceApiError) {
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

  const districts = [...new Set(catalog.content.map((place) => place.district))].sort(
    (left, right) => left.localeCompare(right, "vi"),
  );

  return (
    <PlacesExplorer
      result={result}
      categories={categories}
      districts={districts}
      filters={filters}
    />
  );
}
