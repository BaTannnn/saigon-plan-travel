import Link from "next/link";
import { Card } from "@/components/ui/card";
import { PinIcon } from "@/components/ui/icons";

type AuthCardProps = {
  eyebrow: string;
  title: string;
  description: string;
  alternateText: string;
  alternateHref: string;
  alternateLabel: string;
  children: React.ReactNode;
};

export function AuthCard({
  eyebrow,
  title,
  description,
  alternateText,
  alternateHref,
  alternateLabel,
  children,
}: AuthCardProps) {
  return (
    <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center bg-[linear-gradient(145deg,var(--primary-soft),var(--background)_52%,var(--accent-soft))] px-4 py-10 max-md:min-h-[calc(100dvh_-_4rem)] max-md:py-6">
      <Card className="w-full max-w-[480px] rounded-mint-lg border-border bg-surface p-8 shadow-mint-md ring-0 max-md:p-5">
        <div className="mb-6">
          <span className="mb-4 grid size-12 -rotate-6 place-items-center rounded-[50%_50%_50%_12px] bg-primary-soft text-primary">
            <PinIcon className="size-7" />
          </span>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            {eyebrow}
          </p>
          <h1 className="mt-1.5 mb-2 text-[clamp(1.8rem,5vw,2.45rem)] leading-[1.12] font-bold tracking-[-0.045em]">
            {title}
          </h1>
          <p className="m-0 leading-7 text-text-secondary">{description}</p>
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
