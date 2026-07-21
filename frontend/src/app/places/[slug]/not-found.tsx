import Link from "next/link";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ArrowLeftIcon } from "@/components/ui/icons";

export default function PlaceNotFound() {
  return (
    <main className="page-state-shell">
      <Alert className="error-state">
        <span className="error-state-mark" aria-hidden="true">404</span>
        <p className="eyebrow">Không tìm thấy</p>
        <AlertTitle><h1>Địa điểm này không tồn tại hoặc chưa hoạt động</h1></AlertTitle>
        <AlertDescription>
          <p>Slug phải khớp chính xác với một địa điểm công khai từ Place API.</p>
        </AlertDescription>
        <Button asChild>
          <Link href="/places">
            <ArrowLeftIcon /> Quay lại khám phá
          </Link>
        </Button>
      </Alert>
    </main>
  );
}
