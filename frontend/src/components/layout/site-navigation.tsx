"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const navigationItems = [
  {
    href: "/places",
    label: "Khám phá",
    active: (pathname: string) =>
      pathname === "/places" || pathname.startsWith("/places/"),
  },
  {
    href: "/trips",
    label: "Chuyến đi",
    active: (pathname: string) =>
      pathname === "/trips" ||
      pathname.startsWith("/trips/"),
  },
];

export function SiteNavigation() {
  const pathname = usePathname();

  return (
    <nav
      className="flex h-full items-center gap-9 max-md:hidden"
      aria-label="Điều hướng chính"
    >
      {navigationItems.map((item) => {
        const active = item.active(pathname);

        return (
          <Link
            key={item.href}
            className={cn(
              "relative flex h-full items-center gap-2 font-bold transition-colors",
              active
                ? "text-primary-strong after:absolute after:inset-x-0 after:bottom-0 after:h-[3px] after:rounded-t-[3px] after:bg-primary"
                : "text-text-secondary hover:text-primary-strong",
            )}
            href={item.href}
            aria-current={active ? "page" : undefined}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
