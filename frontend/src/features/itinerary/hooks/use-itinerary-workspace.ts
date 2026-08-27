import { useCallback, useState } from "react";
import type { RunAuthenticated } from "@/features/auth/auth-provider";
import { ApiError } from "@/lib/api/api-client";
import {
  addItineraryItem,
  applyGeneratedItinerary,
  deleteItineraryItem,
  generateItineraryPreview,
  getItinerary,
  reorderItineraryItems,
  replaceItineraryItem,
} from "@/lib/api/itinerary-api";
import type { PlaceSummary } from "@/types/place";
import type { ItineraryResponse } from "@/types/itinerary";

type UseItineraryWorkspaceOptions = {
  tripPublicId: string;
  authenticated: boolean;
  runAuthenticated: RunAuthenticated;
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

export function useItineraryWorkspace({
  tripPublicId,
  authenticated,
  runAuthenticated,
}: UseItineraryWorkspaceOptions) {
  const [itinerary, setItinerary] = useState<ItineraryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [selectedItemPublicId, setSelectedItemPublicId] = useState<
    string | null
  >(null);

  const load = useCallback(async () => {
    if (!authenticated) return;

    setLoading(true);
    setError(null);

    try {
      const response = await runAuthenticated((token) =>
        getItinerary(tripPublicId, token),
      );
      setItinerary(response);
      setLoaded(true);
    } catch (loadError) {
      setError(getItineraryErrorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, [authenticated, runAuthenticated, tripPublicId]);

  const loadIfNeeded = useCallback(() => {
    if (!loaded) void load();
  }, [load, loaded]);

  async function runMutation(
    request: (token: string) => Promise<ItineraryResponse>,
    onSuccess?: (updated: ItineraryResponse) => void,
  ) {
    setMutating(true);
    setError(null);

    try {
      const updated = await runAuthenticated(request);
      setItinerary(updated);
      onSuccess?.(updated);
    } catch (mutationError) {
      setError(getItineraryErrorMessage(mutationError));
      throw mutationError;
    } finally {
      setMutating(false);
    }
  }

  function addPlace(place: PlaceSummary) {
    return runMutation(
      (token) =>
        addItineraryItem(tripPublicId, { placeId: place.id }, token),
      (updated) => {
        const addedItem = updated.items.find(
          (item) => item.place.slug === place.slug,
        );
        setSelectedItemPublicId(addedItem?.publicId ?? null);
      },
    );
  }

  function deleteItem(itemPublicId: string) {
    return runMutation(
      (token) => deleteItineraryItem(tripPublicId, itemPublicId, token),
      () => {
        setSelectedItemPublicId((selected) =>
          selected === itemPublicId ? null : selected,
        );
      },
    );
  }

  function replacePlace(itemPublicId: string, place: PlaceSummary) {
    return runMutation(
      (token) =>
        replaceItineraryItem(
          tripPublicId,
          itemPublicId,
          { placeId: place.id },
          token,
        ),
      () => setSelectedItemPublicId(itemPublicId),
    );
  }

  function reorderItems(itemPublicIds: string[]) {
    return runMutation((token) =>
      reorderItineraryItems(tripPublicId, { itemPublicIds }, token),
    );
  }

  function generatePreview(preferenceDescription: string) {
    return runAuthenticated((token) =>
      generateItineraryPreview(
        tripPublicId,
        { preferenceDescription },
        token,
      ),
    );
  }

  async function applyPreview(placeSlugs: string[]) {
    const updated = await runAuthenticated((token) =>
      applyGeneratedItinerary(tripPublicId, { placeSlugs }, token),
    );
    setItinerary(updated);
  }

  return {
    itinerary,
    loading,
    error,
    mutating,
    selectedItemPublicId,
    setSelectedItemPublicId,
    load,
    loadIfNeeded,
    addPlace,
    deleteItem,
    replacePlace,
    reorderItems,
    generatePreview,
    applyPreview,
  };
}
