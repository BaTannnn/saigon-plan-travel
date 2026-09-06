import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";

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
      <Card className="w-full max-w-[440px] border border-border/50 bg-card p-7 sm:p-8">
        <div className="mb-7">
          <div className="mb-5 flex items-center gap-2 text-primary">
            <Image
              src="/saigonplantravel-logo-icon.svg"
              alt=""
              width={32}
              height={32}
              className="size-8 shrink-0"
            />
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
