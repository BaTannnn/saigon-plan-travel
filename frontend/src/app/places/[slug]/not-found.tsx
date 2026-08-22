import Link from "next/link";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ArrowLeftIcon } from "@/components/ui/icons";

export default function PlaceNotFound() {
  return (
    <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center p-6 max-md:min-h-[calc(100dvh_-_4rem)]">
      <Alert className="w-[min(580px,100%)] rounded-mint-lg border-border bg-surface p-11 text-center shadow-mint-md max-md:p-5 max-md:py-[30px]">
        <span
          className="mx-auto mb-[18px] grid size-[62px] place-items-center rounded-[50%_50%_50%_14px] bg-accent font-black text-surface"
          aria-hidden="true"
        >
          404
        </span>
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
          Không tìm thấy
        </p>
        <AlertTitle>
          <h1 className="mt-[7px] mb-2.5 text-[clamp(1.7rem,4vw,2.5rem)] leading-[1.15] font-bold">
            Địa điểm này không tồn tại hoặc chưa hoạt động
          </h1>
        </AlertTitle>
        <AlertDescription className="leading-[1.65] text-text-secondary">
          <p>
            Slug phải khớp chính xác với một địa điểm công khai từ Place API.
          </p>
        </AlertDescription>
        <Button asChild className="mt-4 justify-self-center">
          <Link href="/">
            <ArrowLeftIcon /> Quay lại khám phá
          </Link>
        </Button>
      </Alert>
    </main>
  );
}
