import Link from "next/link";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { PinIcon } from "@/components/ui/icons";

export function SiteHeader() {
  return (
    <header className="relative z-[1000] grid h-20 grid-cols-[1fr_auto_1fr] items-center border-b border-border bg-white/95 px-7 max-md:h-16 max-md:grid-cols-[1fr_auto] max-md:px-3.5">
      <Link
        className="flex items-center justify-self-start gap-2.5 text-xl font-extrabold tracking-[-0.035em] text-primary-strong max-md:text-base"
        href="/places"
        aria-label="SaigonPlanTravel - Khám phá"
      >
        <span className="grid size-[42px] -rotate-6 place-items-center rounded-[50%_50%_50%_12px] bg-primary-soft text-primary max-md:size-9">
          <PinIcon className="size-[25px]" />
        </span>
        <span>SaigonPlanTravel</span>
      </Link>

      <nav
        className="flex h-full items-center gap-9 max-md:hidden"
        aria-label="Điều hướng chính"
      >
        <Link
          className="relative flex h-full items-center gap-2 font-bold text-primary-strong after:absolute after:inset-x-0 after:bottom-0 after:h-[3px] after:rounded-t-[3px] after:bg-primary"
          href="/places"
        >
          Khám phá
        </Link>
        <span
          className="flex h-full cursor-not-allowed items-center gap-2 font-bold text-text-secondary opacity-60"
          aria-disabled="true"
        >
          Lịch trình
          <Badge
            className="rounded-full px-1.5 py-0.5 text-[0.62rem] text-primary-strong"
            variant="secondary"
          >
            Sắp ra mắt
          </Badge>
        </span>
      </nav>

      <div
        className="flex items-center justify-self-end gap-2 font-bold"
        aria-label="Người dùng Tân"
      >
        <Avatar
          className="size-[42px] border border-border bg-[linear-gradient(145deg,var(--primary-soft),var(--surface))] text-primary-strong max-md:size-[38px]"
          size="lg"
          aria-hidden="true"
        >
          <AvatarFallback className="bg-transparent font-extrabold text-primary-strong">
            T
          </AvatarFallback>
        </Avatar>
        <span className="max-md:hidden">Tân</span>
      </div>
    </header>
  );
}
