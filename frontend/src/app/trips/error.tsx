"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

export default function TripsError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center p-5 max-md:min-h-[calc(100dvh_-_4rem)]">
      <Alert className="max-w-xl rounded-mint-lg border-border bg-surface p-6 shadow-mint-md">
        <AlertTitle>Không thể chuẩn bị biểu mẫu chuyến đi</AlertTitle>
        <AlertDescription>
          Không thể tải dữ liệu chuyến đi. Hãy kiểm tra kết nối và thử lại.
        </AlertDescription>
        <div className="mt-5 flex gap-3">
          <Button type="button" onClick={reset}>Thử lại</Button>
          <Button asChild variant="outline"><Link href="/places">Về khám phá</Link></Button>
        </div>
      </Alert>
    </main>
  );
}
