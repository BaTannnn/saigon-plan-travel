import Link from "next/link";
import { LayoutDashboard, ListOrdered } from "lucide-react";
import { cn } from "@/lib/utils";

export type TripWorkspaceSection = "overview" | "itinerary";

type TripWorkspaceSidebarProps = {
  section: TripWorkspaceSection;
  onSectionChange: (section: TripWorkspaceSection) => void;
};

const sections: Array<{
  value: TripWorkspaceSection;
  label: string;
  icon: typeof LayoutDashboard;
}> = [
  {
    value: "overview",
    label: "Tổng quan",
    icon: LayoutDashboard,
  },
  {
    value: "itinerary",
    label: "Hành trình",
    icon: ListOrdered,
  },
];

export function TripWorkspaceSidebar({
  section,
  onSectionChange,
}: TripWorkspaceSidebarProps) {
  return (
    <aside className="border-b border-border bg-surface/70 px-4 py-4 xl:min-h-0 xl:border-r xl:border-b-0 xl:px-4 xl:py-6">
      <Link
        href="/trips"
        className="inline-flex items-center text-sm font-semibold text-text-secondary transition-colors hover:text-primary-strong xl:px-2"
      >
        ← Chuyến đi của tôi
      </Link>

      <div className="mt-4 flex gap-2 xl:mt-8 xl:flex-col">
        {sections.map(({ value, label, icon: Icon }) => {
          const active = section === value;

          return (
            <button
              key={value}
              type="button"
              className={cn(
                "flex min-h-11 flex-1 items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-bold transition-colors xl:flex-none",
                active
                  ? "bg-primary-soft text-primary-strong"
                  : "text-text-secondary hover:bg-muted/70 hover:text-text-primary",
              )}
              aria-current={active ? "page" : undefined}
              onClick={() => onSectionChange(value)}
            >
              <Icon className="size-4.5" aria-hidden="true" />
              {label}
            </button>
          );
        })}
      </div>
    </aside>
  );
}
