import Link from "next/link";
import { Card } from "@/components/ui/card";
import { PinIcon } from "@/components/ui/icons";

type AuthCardProps = {
  title: string;
  alternateText: string;
  alternateHref: string;
  alternateLabel: string;
  children: React.ReactNode;
};

export function AuthCard({
  title,
  alternateText,
  alternateHref,
  alternateLabel,
  children,
}: AuthCardProps) {
  return (
    <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center bg-background px-4 py-8 sm:px-6 sm:py-10 max-md:min-h-[calc(100dvh_-_4rem)] max-md:py-5">
      <Card className="w-full max-w-[440px] rounded-mint-md border-border bg-surface p-7 shadow-mint-sm ring-0 sm:p-8">
        <div className="mb-7">
          <div className="mb-5 flex items-center gap-2 text-primary">
            <span className="grid size-8 place-items-center rounded-md bg-primary-soft">
              <PinIcon className="size-4" />
            </span>
            <span className="text-sm font-bold tracking-[-0.02em]">
              SaigonPlanTravel
            </span>
          </div>
          <h1 className="m-0 text-[clamp(1.75rem,5vw,2.15rem)] leading-[1.12] font-bold tracking-[-0.04em]">
            {title}
          </h1>
        </div>

        {children}

        <p className="mt-6 mb-0 text-center text-sm text-text-secondary">
          {alternateText}{" "}
          <Link
            className="font-extrabold text-primary underline underline-offset-4"
            href={alternateHref}
          >
            {alternateLabel}
          </Link>
        </p>
      </Card>
    </main>
  );
}
