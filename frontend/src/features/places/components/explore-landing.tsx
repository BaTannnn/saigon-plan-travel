import Link from "next/link";
import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  Map,
  MapPin,
  Plus,
  Search,
} from "lucide-react";
import { Button } from "@/components/ui/button";

const steps = [
  {
    icon: CalendarDays,
    title: "Tạo chuyến đi",
    description:
      "Chọn ngày, thời gian, điểm xuất phát và những sở thích quan trọng với bạn.",
  },
  {
    icon: Plus,
    title: "Xây dựng hành trình",
    description:
      "Tìm kiếm, lọc và thêm các địa điểm SaigonPlanTravel vào lịch trình của riêng bạn.",
  },
  {
    icon: Map,
    title: "Theo dõi trên bản đồ",
    description:
      "Các điểm dừng được đánh số và đồng bộ trực tiếp với hành trình đang chỉnh sửa.",
  },
];

export function ExploreLanding() {
  return (
    <main className="overflow-hidden">
      <section className="relative border-b border-border bg-[radial-gradient(circle_at_85%_15%,var(--primary-soft),transparent_32%),linear-gradient(145deg,var(--background),var(--surface))]">
        <div className="mx-auto grid min-h-[calc(100dvh_-_5rem)] w-[min(1180px,calc(100%_-_40px))] grid-cols-[minmax(0,1fr)_minmax(380px,0.9fr)] items-center gap-16 py-16 max-lg:grid-cols-1 max-lg:gap-10 max-md:min-h-0 max-md:w-[min(100%_-_28px,760px)] max-md:py-12">
          <div className="max-w-2xl">
            <h1 className="m-0 text-[clamp(3rem,6vw,5.6rem)] leading-[0.95] font-black tracking-[-0.065em] text-primary-strong">
              Lên kế hoạch khám phá Sài Gòn theo cách của bạn.
            </h1>
            <p className="mt-6 max-w-xl text-[1.05rem] leading-8 text-text-secondary">
              Tạo chuyến đi, chọn những địa điểm bạn muốn ghé và quản lý toàn bộ
              hành trình trên một bản đồ trực quan. Phần tìm kiếm địa điểm được đặt
              ngay trong lúc bạn xây dựng lịch trình, thay vì tách thành một màn
              hình khám phá riêng.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Button asChild size="lg">
                <Link href="/trips/new">
                  Tạo chuyến đi
                  <ArrowRight className="size-4" aria-hidden="true" />
                </Link>
              </Button>
              <Button asChild size="lg" variant="outline">
                <Link href="/trips">Chuyến đi của tôi</Link>
              </Button>
            </div>

            <div className="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-sm font-semibold text-text-secondary">
              <span className="inline-flex items-center gap-2">
                <CheckCircle2 className="size-4 text-primary" />
                Lịch trình có thể chỉnh sửa
              </span>
              <span className="inline-flex items-center gap-2">
                <CheckCircle2 className="size-4 text-primary" />
                Dữ liệu địa điểm có cấu trúc
              </span>
              <span className="inline-flex items-center gap-2">
                <CheckCircle2 className="size-4 text-primary" />
                Bản đồ OpenStreetMap
              </span>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-[520px] max-lg:max-w-[620px]">
            <div className="absolute -top-8 -right-8 size-40 rounded-full bg-primary-soft blur-2xl" />
            <div className="relative overflow-hidden rounded-mint-md border border-border/50 bg-card shadow-mint-sm">
              <div className="grid min-h-[520px] grid-cols-[42%_1fr] max-sm:min-h-[430px]">
                <div className="border-r border-border/50 bg-background p-5">
                  <p className="text-[0.68rem] font-extrabold tracking-[0.12em] text-primary uppercase">
                    Hành trình
                  </p>
                  <h2 className="mt-2 text-2xl font-black tracking-[-0.04em]">
                    Một ngày ở TP.HCM
                  </h2>

                  <div className="mt-7 grid gap-3">
                    {["Chợ Bến Thành", "Bảo tàng", "Không gian nghệ thuật"].map(
                      (name, index) => (
                        <div
                          key={name}
                          className="grid grid-cols-[34px_1fr] items-center gap-2.5 rounded-xl border border-border/40 bg-card p-2.5 shadow-mint-sm"
                        >
                          <span className="grid size-8 place-items-center rounded-full bg-primary text-xs font-black text-primary-foreground">
                            {index + 1}
                          </span>
                          <span className="truncate text-xs font-bold">{name}</span>
                        </div>
                      ),
                    )}
                  </div>

                  <div className="mt-4 flex items-center gap-2 rounded-xl border border-dashed border-primary/35 bg-primary-soft/60 p-3 text-xs font-bold text-primary-strong">
                    <Search className="size-4" />
                    Tìm và thêm địa điểm
                  </div>
                </div>

                <div className="relative overflow-hidden bg-[linear-gradient(135deg,#e8f3ee,#d8ebe5)]">
                  <div className="absolute inset-0 opacity-50 [background-image:linear-gradient(to_right,#8bbdb3_1px,transparent_1px),linear-gradient(to_bottom,#8bbdb3_1px,transparent_1px)] [background-size:34px_34px]" />
                  <div className="absolute top-[28%] left-[36%] grid size-12 place-items-center rounded-full border-4 border-white bg-primary text-lg font-black text-white shadow-mint-md">
                    1
                  </div>
                  <div className="absolute top-[47%] right-[22%] grid size-12 place-items-center rounded-full border-4 border-white bg-accent text-lg font-black text-white shadow-mint-md">
                    2
                  </div>
                  <div className="absolute bottom-[22%] left-[26%] grid size-12 place-items-center rounded-full border-4 border-white bg-primary text-lg font-black text-white shadow-mint-md">
                    3
                  </div>
                  <div className="absolute right-5 bottom-5 left-5 rounded-xl border border-border/40 bg-card p-3 shadow-mint-sm">
                    <div className="flex items-center gap-2 text-xs font-bold text-primary-strong">
                      <MapPin className="size-4" />
                      Lịch trình và bản đồ luôn đồng bộ
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-background py-20 max-md:py-14">
        <div className="mx-auto w-[min(1080px,calc(100%_-_40px))] max-md:w-[min(100%_-_28px,760px)]">
          <div className="max-w-2xl">
            <h2 className="m-0 text-[clamp(2rem,4vw,3.2rem)] leading-tight font-black tracking-[-0.045em]">
              Từ ý tưởng chuyến đi đến một hành trình có thể chỉnh sửa.
            </h2>
          </div>

          <div className="mt-10 grid grid-cols-3 gap-4 max-md:grid-cols-1">
            {steps.map((step, index) => {
              const Icon = step.icon;
              return (
                <article
                  key={step.title}
                  className="rounded-mint-md border border-border/50 bg-card p-6 shadow-mint-sm"
                >
                  <div className="flex items-center justify-between">
                    <span className="grid size-10 place-items-center rounded-full bg-primary-soft text-primary">
                      <Icon className="size-5" />
                    </span>
                    <span className="text-xs font-black tracking-[0.12em] text-text-secondary">
                      0{index + 1}
                    </span>
                  </div>
                  <h3 className="mt-6 mb-0 text-xl font-black tracking-[-0.025em]">
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
    </main>
  );
}
