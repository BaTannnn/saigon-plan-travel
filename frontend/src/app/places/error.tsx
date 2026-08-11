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
    <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center p-6 max-md:min-h-[calc(100dvh_-_4rem)]">
      <Alert className="w-[min(580px,100%)] rounded-mint-lg border-border bg-surface p-11 text-center shadow-mint-md max-md:p-5 max-md:py-[30px]">
        <span
          className="mx-auto mb-[18px] grid size-[62px] place-items-center rounded-[50%_50%_50%_14px] bg-accent font-black text-surface"
          aria-hidden="true"
        >
          !
        </span>
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
          Đã xảy ra lỗi
        </p>
        <AlertTitle>
          <h1 className="mt-[7px] mb-2.5 text-[clamp(1.7rem,4vw,2.5rem)] leading-[1.15] font-bold">
            Chưa thể hiển thị trang khám phá
          </h1>
        </AlertTitle>
        <AlertDescription className="leading-[1.65] text-text-secondary">
          <p>
            Thử tải lại trang. Nếu lỗi tiếp tục, hãy kiểm tra terminal của
            Next.js.
          </p>
        </AlertDescription>
        <Button
          className="mt-4 justify-self-center"
          type="button"
          onClick={reset}
        >
          Thử lại
        </Button>
      </Alert>
    </main>
  );
}
