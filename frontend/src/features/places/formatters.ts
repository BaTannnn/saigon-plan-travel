const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

export function formatCost(minCost: number, maxCost: number) {
  if (minCost === 0 && maxCost === 0) {
    return "Miễn phí";
  }

  if (minCost === maxCost) {
    return currencyFormatter.format(minCost);
  }

  return `${currencyFormatter.format(minCost)} – ${currencyFormatter.format(maxCost)}`;
}

export function formatDuration(minutes: number) {
  if (minutes < 60) {
    return `${minutes} phút`;
  }

  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return remainder ? `${hours} giờ ${remainder} phút` : `${hours} giờ`;
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
