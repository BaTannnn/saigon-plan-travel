"use client";

import { useEffect } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

export default function PlacesError({
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
    <main className="page-state-shell">
      <Alert className="error-state">
        <span className="error-state-mark" aria-hidden="true">!</span>
        <p className="eyebrow">Đã xảy ra lỗi</p>
        <AlertTitle><h1>Chưa thể hiển thị trang khám phá</h1></AlertTitle>
        <AlertDescription>
          <p>Thử tải lại trang. Nếu lỗi tiếp tục, hãy kiểm tra terminal của Next.js.</p>
        </AlertDescription>
        <Button type="button" onClick={reset}>Thử lại</Button>
      </Alert>
    </main>
  );
}
