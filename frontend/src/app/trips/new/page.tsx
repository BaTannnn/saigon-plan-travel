import type { Metadata } from "next";
import { connection } from "next/server";
import { CreateTripView } from "@/features/trips/components/create-trip-view";

export const metadata: Metadata = {
  title: "Tạo chuyến đi",
};

function getInitialDate(value?: string | string[]) {
  const date = Array.isArray(value) ? value[0] : value;
  if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date)) return undefined;

  const [year, month, day] = date.split("-").map(Number);
  const parsedDate = new Date(Date.UTC(year, month - 1, day));
  const isValidDate =
    parsedDate.getUTCFullYear() === year &&
    parsedDate.getUTCMonth() + 1 === month &&
    parsedDate.getUTCDate() === day;

  return isValidDate ? date : undefined;
}

export default async function NewTripPage({
  searchParams,
}: {
  searchParams: Promise<{ date?: string | string[] }>;
}) {
  await connection();
  const { date } = await searchParams;

  return <CreateTripView initialDate={getInitialDate(date)} />;
}
