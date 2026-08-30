const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

const distanceFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 1,
});

export function formatCurrency(value: number) {
  return currencyFormatter.format(value);
}

export function formatCost(minCost: number, maxCost: number) {
  if (minCost === 0 && maxCost === 0) {
    return "Miễn phí";
  }

  if (minCost === maxCost) {
    return formatCurrency(minCost);
  }

  return `${formatCurrency(minCost)} – ${formatCurrency(maxCost)}`;
}

export function formatDuration(minutes: number) {
  if (minutes < 60) {
    return `${minutes} phút`;
  }

  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return remainder ? `${hours} giờ ${remainder} phút` : `${hours} giờ`;
}

export function formatTime(value: string) {
  return value.slice(0, 5);
}

export function formatDistance(value: number) {
  return `${distanceFormatter.format(value)} km`;
}
