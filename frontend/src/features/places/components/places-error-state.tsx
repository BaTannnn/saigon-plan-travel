"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

type PlacesErrorStateProps = {
  connectionFailure: boolean;
  detail?: string;
};

export function PlacesErrorState({ connectionFailure, detail }: PlacesErrorStateProps) {
  const router = useRouter();

  return (
    <main className="page-state-shell">
      <Alert className="error-state">
        <span className="error-state-mark" aria-hidden="true">!</span>
        <p className="eyebrow">Không thể tải địa điểm</p>
        <AlertTitle>
          <h1>
            {connectionFailure
              ? "Chưa kết nối được với backend"
              : "Yêu cầu tìm kiếm chưa hợp lệ"}
          </h1>
        </AlertTitle>
        <AlertDescription>
          <p>
            {connectionFailure
              ? "Hãy kiểm tra Spring Boot đang chạy và BACKEND_API_BASE_URL trỏ đúng địa chỉ. Dữ liệu giả sẽ không được dùng thay thế."
              : detail ?? "Hãy xóa bộ lọc và thử lại."}
          </p>
        </AlertDescription>
        <div className="state-actions">
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
