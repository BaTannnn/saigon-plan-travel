"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { ArrowLeftIcon } from "@/components/ui/icons";

export function PlaceDetailBackButton() {
  const router = useRouter();

  return (
    <Button
      type="button"
      className="mb-[18px] min-h-11 p-0 font-extrabold"
      variant="link"
      onClick={() => router.back()}
    >
      <ArrowLeftIcon /> Quay lại
    </Button>
  );
}
