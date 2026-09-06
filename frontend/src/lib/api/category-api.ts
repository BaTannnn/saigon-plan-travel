import { requestJson } from "@/lib/api/api-client";
import type { Category } from "@/types/category";

export async function getCategories() {
  return requestJson<Category[]>("/api/v1/categories", {
    method: "GET",
    cache: "no-store",
  });
}
