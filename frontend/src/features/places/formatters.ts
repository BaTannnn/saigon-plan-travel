import { formatCurrency } from "@/lib/formatters";

export { formatDuration } from "@/lib/formatters";

export function formatCost(minCost: number, maxCost: number) {
  if (minCost === 0 && maxCost === 0) {
    return "Miễn phí";
  }

  if (minCost === maxCost) {
    return formatCurrency(minCost);
  }

  return `${formatCurrency(minCost)} – ${formatCurrency(maxCost)}`;
}

export const dayNames: Record<number, string> = {
  1: "Thứ Hai",
  2: "Thứ Ba",
  3: "Thứ Tư",
  4: "Thứ Năm",
  5: "Thứ Sáu",
  6: "Thứ Bảy",
  7: "Chủ Nhật",
};
