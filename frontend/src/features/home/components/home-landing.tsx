import Image from "next/image";
import Link from "next/link";
import { ArrowRight, CalendarDays, Map, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

const steps = [
  {
    icon: CalendarDays,
    title: "Tạo chuyến đi",
    description: "Chọn ngày, điểm xuất phát và những điều bạn muốn ưu tiên.",
  },
  {
    icon: Plus,
    title: "Xây dựng hành trình",
    description: "Chọn và sắp xếp các địa điểm theo nhịp điệu của riêng bạn.",
  },
  {
    icon: Map,
    title: "Theo dõi trên bản đồ",
    description: "Theo dõi từng điểm dừng trực tiếp trên bản đồ của chuyến đi.",
  },
];

export function HomeLanding() {
  return (
    <main>
      <section
        className="relative isolate flex min-h-[calc(100dvh_-_5rem)] items-center justify-center overflow-hidden bg-foreground px-6 py-16 text-center max-md:min-h-[calc(100svh_-_4rem)] max-md:px-5 max-md:py-12"
        aria-labelledby="landing-hero-title"
      >
        <Image
          className="object-cover object-center"
          src="/hochiguide.jpg"
          alt=""
          fill
          priority
          sizes="100vw"
        />
        <div className="absolute inset-0 bg-black/50" aria-hidden="true" />
        <div className="relative z-10 max-w-3xl">
          <h1
            id="landing-hero-title"
            className="m-0 text-[clamp(2.5rem,5vw,4.75rem)] leading-[1.02] font-black tracking-[-0.055em] text-white"
          >
            Khám phá Sài Gòn theo cách của bạn
          </h1>
          <p className="mx-auto mt-6 max-w-xl text-[1.05rem] leading-8 text-white/85 max-md:mt-5 max-md:text-base max-md:leading-7">
            Tạo lịch trình trong ngày dựa trên thời gian, ngân sách và sở thích
            của bạn.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3 max-md:mt-7 max-md:grid max-md:grid-cols-1">
            <Button asChild size="lg" variant="accent">
              <Link href="/trips/new">
                Tạo chuyến đi
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
            </Button>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="border-white/70 bg-transparent text-white hover:border-white hover:bg-white hover:text-foreground"
            >
              <Link href="/trips">Chuyến đi của tôi</Link>
            </Button>
          </div>
        </div>
      </section>

      <section className="bg-background py-16 max-md:py-12">
        <div className="mx-auto w-[min(1080px,calc(100%_-_40px))] max-md:w-[min(100%_-_28px,760px)]">
          <div className="max-w-2xl">
            <h2 className="m-0 text-[clamp(2rem,4vw,3rem)] leading-tight font-black tracking-[-0.045em] text-text-primary">
              Lên kế hoạch, theo cách đơn giản hơn.
            </h2>
          </div>

          <div className="mt-10 grid grid-cols-3 gap-4 max-md:grid-cols-1">
            {steps.map((step, index) => {
              const Icon = step.icon;
              return (
                <article
                  key={step.title}
                  className="rounded-mint-md border border-border bg-card p-6 shadow-mint-sm"
                >
                  <div className="flex items-center justify-between">
                    <span className="grid size-10 place-items-center rounded-full bg-primary-soft text-primary">
                      <Icon className="size-5" />
                    </span>
                    <span className="text-xs font-black tracking-[0.12em] text-text-secondary">
                      0{index + 1}
                    </span>
                  </div>
                  <h3 className="mt-5 mb-0 text-xl font-black tracking-[-0.025em] text-text-primary">
                    {step.title}
                  </h3>
                  <p className="mt-3 mb-0 text-sm leading-6 text-text-secondary">
                    {step.description}
                  </p>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <footer className="border-t border-border bg-secondary/55">
        <div className="mx-auto grid w-[min(1080px,calc(100%_-_40px))] grid-cols-[minmax(0,1fr)_auto] gap-8 py-10 max-md:w-[min(100%_-_28px,760px)] max-md:grid-cols-1 max-md:gap-7 max-md:py-8">
          <div>
            <p className="m-0 text-lg font-extrabold tracking-[-0.035em] text-primary-strong">
              SaigonPlanTravel
            </p>
            <p className="mt-2 mb-0 text-sm text-text-secondary">
              Lập kế hoạch cho những ngày khám phá của bạn tại Sài Gòn.
            </p>
          </div>
          <nav aria-label="Điều hướng chân trang">
            <ul className="m-0 flex list-none flex-wrap gap-x-6 gap-y-3 p-0 text-sm font-semibold">
              <li>
                <Link
                  className="text-text-secondary transition-colors hover:text-primary"
                  href="/"
                >
                  Khám phá
                </Link>
              </li>
              <li>
                <Link
                  className="text-text-secondary transition-colors hover:text-primary"
                  href="/trips"
                >
                  Chuyến đi
                </Link>
              </li>
              <li>
                <Link
                  className="text-text-secondary transition-colors hover:text-primary"
                  href="/trips/new"
                >
                  Tạo chuyến đi
                </Link>
              </li>
            </ul>
          </nav>
        </div>
        <div className="border-t border-border/80">
          <p className="mx-auto w-[min(1080px,calc(100%_-_40px))] py-4 text-xs text-text-secondary max-md:w-[min(100%_-_28px,760px)]">
            © 2026 SaigonPlanTravel
          </p>
        </div>
      </footer>
    </main>
  );
}
