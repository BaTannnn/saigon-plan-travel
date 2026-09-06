import Image from "next/image";
import Link from "next/link";
import { AuthControls } from "@/components/layout/auth-controls";
import { SiteNavigation } from "@/components/layout/site-navigation";

export function SiteHeader() {
  return (
    <header className="relative z-[1000] grid h-20 grid-cols-[1fr_auto_1fr] items-center border-b border-border bg-white/95 px-7 max-md:h-16 max-md:grid-cols-[1fr_auto] max-md:px-3.5">
      <Link
        className="flex items-center justify-self-start gap-2.5 text-xl font-extrabold tracking-[-0.035em] text-primary-strong max-md:text-base"
        href="/"
        aria-label="SaigonPlanTravel - Khám phá"
      >
        <Image
          src="/saigonplantravel-logo-icon.svg"
          alt=""
          width={42}
          height={42}
          className="size-[42px] shrink-0 max-md:size-9"
          priority
        />
        <span>SaigonPlanTravel</span>
      </Link>

      <SiteNavigation />

      <AuthControls />
    </header>
  );
}
