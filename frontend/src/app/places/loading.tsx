import { Skeleton } from "@/components/ui/skeleton";

export default function PlacesLoading() {
  return (
    <main className="mx-auto grid min-h-[calc(100dvh_-_5rem)] w-[min(1180px,calc(100%_-_40px))] grid-cols-2 items-center gap-16 py-16 max-lg:grid-cols-1 max-md:min-h-0 max-md:w-[min(100%_-_28px,760px)] max-md:py-12">
      <section>
        <Skeleton className="h-4 w-36" />
        <Skeleton className="mt-5 h-20 w-full max-w-[620px] rounded-xl" />
        <Skeleton className="mt-3 h-20 w-[86%] max-w-[560px] rounded-xl" />
        <Skeleton className="mt-6 h-24 w-full max-w-[590px] rounded-xl" />
        <div className="mt-8 flex gap-3">
          <Skeleton className="h-14 w-40 rounded-lg" />
          <Skeleton className="h-14 w-40 rounded-lg" />
        </div>
      </section>
      <Skeleton className="min-h-[520px] rounded-[28px]" />
    </main>
  );
}
