import type {
  ApiProblem,
  Category,
  PlaceDetail,
  PlacePage,
  PlacesSearchFilters,
} from "@/types/place";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

export class PlaceApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: ApiProblem | null,
  ) {
    super(problem?.detail ?? "Không thể kết nối đến Place API.");
    this.name = "PlaceApiError";
  }
}

function getBackendBaseUrl() {
  return (process.env.BACKEND_API_BASE_URL ?? DEFAULT_BACKEND_URL).replace(
    /\/$/,
    "",
  );
}

async function requestJson<T>(path: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${getBackendBaseUrl()}${path}`, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
  } catch {
    throw new PlaceApiError(0, null);
  }

  if (!response.ok) {
    let problem: ApiProblem | null = null;

    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      // The status still provides a useful failure signal when the body is not JSON.
    }

    throw new PlaceApiError(response.status, problem);
  }

  return (await response.json()) as T;
}

function appendIfPresent(
  params: URLSearchParams,
  key: string,
  value: string | number | undefined,
) {
  if (value === undefined || String(value).trim() === "") {
    return;
  }

  params.set(key, String(value));
}

export async function getPlaces(filters: PlacesSearchFilters) {
  const params = new URLSearchParams();
  appendIfPresent(params, "keyword", filters.keyword);
  appendIfPresent(params, "district", filters.district);
  appendIfPresent(params, "category", filters.category);
  appendIfPresent(params, "indoor", filters.indoor);
  appendIfPresent(params, "maxCost", filters.maxCost);
  appendIfPresent(params, "page", filters.page);
  appendIfPresent(params, "size", filters.size);

  return requestJson<PlacePage>(`/api/v1/places?${params.toString()}`);
}

export async function getPlaceCatalog() {
  return requestJson<PlacePage>("/api/v1/places?page=0&size=100");
}

export async function getCategories() {
  return requestJson<Category[]>("/api/v1/categories");
}

export async function getPlaceDetail(slug: string) {
  return requestJson<PlaceDetail>(`/api/v1/places/${encodeURIComponent(slug)}`);
}
