import { requestJson } from "@/lib/api/place-api";
import type { Category } from "@/types/category";

export async function getCategories() {
  return requestJson<Category[]>("/api/v1/categories");
}
