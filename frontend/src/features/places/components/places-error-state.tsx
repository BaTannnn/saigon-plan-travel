"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

type PlacesErrorStateProps = {
  connectionFailure: boolean;
  detail?: string;
};

export function PlacesErrorState({
  connectionFailure,
  detail,
}: PlacesErrorStateProps) {
  const router = useRouter();

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
          Không thể tải địa điểm
        </p>
        <AlertTitle>
          <h1 className="mt-[7px] mb-2.5 text-[clamp(1.7rem,4vw,2.5rem)] leading-[1.15] font-bold">
            {connectionFailure
              ? "Chưa kết nối được với backend"
              : "Yêu cầu tìm kiếm chưa hợp lệ"}
          </h1>
        </AlertTitle>
        <AlertDescription className="leading-[1.65] text-text-secondary">
          <p>
            {connectionFailure
              ? "Hãy kiểm tra Spring Boot đang chạy và BACKEND_API_BASE_URL trỏ đúng địa chỉ. Dữ liệu giả sẽ không được dùng thay thế."
              : (detail ?? "Hãy xóa bộ lọc và thử lại.")}
          </p>
        </AlertDescription>
        <div className="mt-[22px] flex justify-center gap-2.5 max-md:flex-col">
          <Button type="button" onClick={() => router.refresh()}>
            Thử lại
          </Button>
          <Button asChild variant="outline">
            <Link href="/places">Xóa bộ lọc</Link>
          </Button>
        </div>
      </Alert>
    </main>
  );
}
