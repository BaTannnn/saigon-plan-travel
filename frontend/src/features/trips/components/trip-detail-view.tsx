"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";
import { ItineraryView } from "@/features/itinerary/components/itinerary-view";
import { ItineraryMapShell } from "@/features/itinerary/map/itinerary-map-shell";
import { TripForm } from "@/features/trips/components/trip-form";
import { TripReview } from "@/features/trips/components/trip-review";
import {
  TripWorkspaceSidebar,
  type TripWorkspaceSection,
} from "@/features/trips/components/trip-workspace-sidebar";
import { getTripLoadErrorMessage } from "@/features/trips/trip-errors";
import { TripOriginMapShell } from "@/features/trips/map/trip-origin-map-shell";
import { ApiError } from "@/lib/api/api-client";
import {
  addItineraryItem,
  deleteItineraryItem,
  getItinerary,
  replaceItineraryItem,
} from "@/lib/api/itinerary-api";
import { getTrip, replaceTrip } from "@/lib/api/trip-api";
import type { Category } from "@/types/category";
import type { ItineraryResponse } from "@/types/itinerary";
import type { PlaceSummary } from "@/types/place";
import type { SaveTripRequest, TripResponse } from "@/types/trip";

type TripDetailViewProps = {
  publicId: string;
  categories: Category[];
};

function getItineraryErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Không tìm thấy chuyến đi hoặc hành trình này.";
    }
    if (error.status === 409) {
      return error.problem?.detail ?? "Hành trình không thể cập nhật lúc này.";
    }
    if (error.status === 0) {
      return "Không thể kết nối đến máy chủ.";
    }
    return error.problem?.detail ?? "Không thể tải hành trình.";
  }

  return "Đã xảy ra lỗi khi xử lý hành trình.";
}

export function TripDetailView({ publicId, categories }: TripDetailViewProps) {
  const router = useRouter();
  const { status, runAuthenticated } = useAuth();
  const [trip, setTrip] = useState<TripResponse | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [section, setSection] = useState<TripWorkspaceSection>("overview");
  const [itinerary, setItinerary] = useState<ItineraryResponse | null>(null);
  const [itineraryLoading, setItineraryLoading] = useState(false);
  const [itineraryError, setItineraryError] = useState<string | null>(null);
  const [itineraryLoaded, setItineraryLoaded] = useState(false);
  const [itineraryMutating, setItineraryMutating] = useState(false);
  const [selectedItemPublicId, setSelectedItemPublicId] = useState<
    string | null
  >(null);

  useEffect(() => {
    if (status === "guest") {
      router.replace("/login");
      return;
    }
    if (status !== "authenticated") return;

    let cancelled = false;

    runAuthenticated((token) => getTrip(publicId, token))
      .then((response) => {
        if (cancelled) return;
        setTrip(response);
      })
      .catch((error) => {
        if (cancelled) return;
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError(getTripLoadErrorMessage(error));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [publicId, router, runAuthenticated, status]);

  const loadItinerary = useCallback(async () => {
    if (status !== "authenticated") return;

    setItineraryLoading(true);
    setItineraryError(null);

    try {
      const response = await runAuthenticated((token) =>
        getItinerary(publicId, token),
      );
      setItinerary(response);
      setItineraryLoaded(true);
    } catch (error) {
      setItineraryError(getItineraryErrorMessage(error));
    } finally {
      setItineraryLoading(false);
    }
  }, [publicId, runAuthenticated, status]);


  async function handleReplace(request: SaveTripRequest) {
    const updated = await runAuthenticated((token) =>
      replaceTrip(publicId, request, token),
    );
    setTrip(updated);
    setEditing(false);
  }

  async function handleAddPlace(place: PlaceSummary) {
    setItineraryMutating(true);
    setItineraryError(null);

    try {
      const updated = await runAuthenticated((token) =>
        addItineraryItem(publicId, { placeId: place.id }, token),
      );
      setItinerary(updated);
      const addedItem = updated.items.find((item) => item.place.id === place.id);
      setSelectedItemPublicId(addedItem?.publicId ?? null);
    } catch (error) {
      setItineraryError(getItineraryErrorMessage(error));
      throw error;
    } finally {
      setItineraryMutating(false);
    }
  }

  async function handleDeleteItem(itemPublicId: string) {
    setItineraryMutating(true);
    setItineraryError(null);

    try {
      const updated = await runAuthenticated((token) =>
        deleteItineraryItem(publicId, itemPublicId, token),
      );
      setItinerary(updated);
      if (selectedItemPublicId === itemPublicId) {
        setSelectedItemPublicId(null);
      }
    } catch (error) {
      setItineraryError(getItineraryErrorMessage(error));
      throw error;
    } finally {
      setItineraryMutating(false);
    }
  }

  async function handleReplacePlace(
    itemPublicId: string,
    place: PlaceSummary,
  ) {
    setItineraryMutating(true);
    setItineraryError(null);

    try {
      const updated = await runAuthenticated((token) =>
        replaceItineraryItem(
          publicId,
          itemPublicId,
          { placeId: place.id },
          token,
        ),
      );
      setItinerary(updated);
      setSelectedItemPublicId(itemPublicId);
    } catch (error) {
      setItineraryError(getItineraryErrorMessage(error));
      throw error;
    } finally {
      setItineraryMutating(false);
    }
  }

  function handleSectionChange(nextSection: TripWorkspaceSection) {
  setEditing(false);
  setSection(nextSection);

  if (nextSection === "overview") {
    setSelectedItemPublicId(null);
    return;
  }

  if (!itineraryLoaded) {
    void loadItinerary();
  }
}
  if (status === "loading" || loading) {
    return (
      <main
        className="mx-auto grid w-[min(1120px,calc(100%_-_32px))] gap-4 py-10"
        aria-label="Đang tải chuyến đi"
      >
        <Skeleton className="h-52 rounded-mint-lg" />
        <Skeleton className="h-64 rounded-mint-md" />
      </main>
    );
  }

  if (loadError || !trip) {
    return (
      <main className="grid min-h-[calc(100dvh_-_5rem)] place-items-center p-5 max-md:min-h-[calc(100dvh_-_4rem)]">
        <Alert className="max-w-xl rounded-mint-lg border-border bg-surface p-6 shadow-mint-md">
          <AlertTitle>Không thể mở chuyến đi</AlertTitle>
          <AlertDescription>
            {loadError ?? "Chuyến đi không khả dụng."}
          </AlertDescription>
          <div className="mt-5 flex gap-3">
            <Button asChild>
              <Link href="/trips/new">Tạo chuyến đi mới</Link>
            </Button>
            <Button asChild variant="outline">
              <Link href="/places">Về khám phá</Link>
            </Button>
          </div>
        </Alert>
      </main>
    );
  }

  const overviewContent = editing ? (
    <>
      <header className="mb-7">
        <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
          PUT · Thay thế đầy đủ
        </p>
        <h1 className="mt-1.5 mb-2 text-[clamp(2rem,5vw,3.2rem)] leading-[1.08] font-bold tracking-[-0.05em]">
          Chỉnh sửa chuyến đi
        </h1>
        <p className="m-0 text-text-secondary">
          Khi lưu, toàn bộ trường bên dưới sẽ thay thế phiên bản hiện tại.
        </p>
      </header>
      <TripForm
        categories={categories}
        initialTrip={trip}
        submitLabel="Lưu thay đổi"
        onSubmit={handleReplace}
        onCancel={() => setEditing(false)}
      />
    </>
  ) : (
    <TripReview trip={trip} onEdit={() => setEditing(true)} />
  );

  const itineraryItems = itinerary?.items ?? [];

  return (
    <main className="grid min-h-[calc(100dvh_-_4rem)] grid-cols-1 bg-background md:min-h-[calc(100dvh_-_5rem)] xl:h-[calc(100dvh_-_5rem)] xl:min-h-[680px] xl:grid-cols-[210px_clamp(470px,34vw,580px)_minmax(0,1fr)] xl:overflow-hidden">
      <TripWorkspaceSidebar
        section={section}
        onSectionChange={handleSectionChange}
      />

      <section className="min-w-0 px-5 py-8 sm:px-8 md:px-10 xl:min-h-0 xl:overflow-y-auto xl:px-9 xl:py-10">
        {section === "overview" ? (
          overviewContent
        ) : (
          <ItineraryView
            trip={trip}
            itinerary={itinerary}
            loading={itineraryLoading}
            error={itineraryError}
            selectedItemPublicId={selectedItemPublicId}
            mutating={itineraryMutating}
            onRetry={loadItinerary}
            onAdd={handleAddPlace}
            onDelete={handleDeleteItem}
            onReplace={handleReplacePlace}
            onSelectItem={setSelectedItemPublicId}
          />
        )}
      </section>

      <section
        className="h-[360px] min-w-0 border-t border-border md:h-[440px] xl:h-auto xl:min-h-0 xl:border-t-0 xl:border-l"
        aria-label={
          section === "overview"
            ? "Bản đồ điểm xuất phát"
            : "Bản đồ hành trình"
        }
      >
        {section === "overview" ? (
          <TripOriginMapShell
            latitude={trip.startLocation.latitude}
            longitude={trip.startLocation.longitude}
            label={trip.startLocation.label}
          />
        ) : (
          <ItineraryMapShell
            origin={trip.startLocation}
            items={itineraryItems}
            selectedItemPublicId={selectedItemPublicId}
            onSelectItem={setSelectedItemPublicId}
          />
        )}
      </section>
    </main>
  );
}
