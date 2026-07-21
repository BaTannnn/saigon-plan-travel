import { Skeleton } from "@/components/ui/skeleton";

export default function PlacesLoading() {
  return (
    <main className="places-loading" aria-busy="true" aria-label="Đang tải địa điểm">
      <section className="loading-sidebar">
        <Skeleton className="skeleton-title" />
        <Skeleton className="skeleton-button" />
        <Skeleton className="skeleton-row" />
        {[1, 2, 3].map((item) => (
          <Skeleton className="skeleton-card" key={item} />
        ))}
      </section>
      <section className="map-loading">
        <span className="loading-spinner" /> Đang tải bản đồ…
      </section>
    </main>
  );
}
