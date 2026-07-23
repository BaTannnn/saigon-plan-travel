import { Skeleton } from "@/components/ui/skeleton";

export default function PlacesLoading() {
  return (
    <main
      className="grid h-[calc(100dvh_-_5rem)] min-h-[620px] grid-cols-[minmax(410px,37%)_1fr] overflow-hidden md:max-[1100px]:grid-cols-[minmax(370px,42%)_1fr] max-md:block max-md:h-auto max-md:min-h-[calc(100dvh_-_4rem)] max-md:overflow-visible"
      aria-busy="true"
      aria-label="Đang tải địa điểm"
    >
      <section className="px-7 py-[34px] max-md:min-h-[calc(100dvh_-_4rem)] max-md:px-4 max-md:py-6">
        <Skeleton className="h-[76px] w-[78%] rounded-lg" />
        <Skeleton className="mt-[22px] h-[54px] rounded-lg" />
        <Skeleton className="my-[18px] h-10 rounded-lg" />
        {[1, 2, 3].map((item) => (
          <Skeleton className="mb-3 h-[148px] rounded-lg" key={item} />
        ))}
      </section>
      <section className="flex size-full items-center justify-center gap-2.5 bg-[linear-gradient(135deg,var(--primary-soft),var(--background))] text-primary-strong max-md:hidden">
        <span className="size-6 animate-spin rounded-full border-[3px] border-border border-t-primary motion-reduce:animate-none" />{" "}
        Đang tải bản đồ…
      </section>
    </main>
  );
}
