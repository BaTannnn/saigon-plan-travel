import { requestJson } from "@/lib/api/api-client";
import type { Category } from "@/types/category";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

function getBackendBaseUrl() {
  return (process.env.BACKEND_API_BASE_URL ?? DEFAULT_BACKEND_URL).replace(
    /\/$/,
    "",
  );
}

export async function getCategories() {
  return requestJson<Category[]>(`${getBackendBaseUrl()}/api/v1/categories`, {
    cache: "no-store",
  });
}
