"use client";

import { useState, type FormEvent, type KeyboardEvent } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  CalendarIcon,
  ClockIcon,
  PinIcon,
  SearchIcon,
  WalletIcon,
} from "@/components/ui/icons";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { getTripFormFeedback } from "@/features/trips/trip-errors";
import { ApiError } from "@/lib/api/api-client";
import type { LocationSearchResult } from "@/types/location";
import type {
  EnvironmentPreference,
  SaveTripRequest,
  TravelPace,
  TripResponse,
} from "@/types/trip";

type TripFormValues = {
  tripDate: string;
  startTime: string;
  endTime: string;
  budget: string;
  originLabel: string;
  latitude: string;
  longitude: string;
  travelPace: TravelPace;
  environmentPreference: EnvironmentPreference;
};

type TripFormProps = {
  initialTrip?: TripResponse;
  submitLabel: string;
  onSubmit: (request: SaveTripRequest) => Promise<void>;
  onSearchLocations: (query: string) => Promise<LocationSearchResult[]>;
  onCancel?: () => void;
};

const paceLabels: Record<TravelPace, string> = {
  RELAXED: "Thư thả",
  BALANCED: "Cân bằng",
  FAST: "Nhanh",
};

const environmentLabels: Record<EnvironmentPreference, string> = {
  INDOOR: "Trong nhà",
  OUTDOOR: "Ngoài trời",
  MIXED: "Kết hợp",
};

function initialValues(initialTrip?: TripResponse): TripFormValues {
  if (!initialTrip) {
    return {
      tripDate: "",
      startTime: "08:00",
      endTime: "18:00",
      budget: "",
      originLabel: "",
      latitude: "",
      longitude: "",
      travelPace: "BALANCED",
      environmentPreference: "MIXED",
    };
  }

  return {
    tripDate: initialTrip.tripDate,
    startTime: initialTrip.startTime,
    endTime: initialTrip.endTime,
    budget: String(initialTrip.budget),
    originLabel: initialTrip.startLocation.label,
    latitude: String(initialTrip.startLocation.latitude),
    longitude: String(initialTrip.startLocation.longitude),
    travelPace: initialTrip.travelPace,
    environmentPreference: initialTrip.environmentPreference,
  };
}

function toRequest(
  values: TripFormValues,
  latitude: number,
  longitude: number,
): SaveTripRequest {
  return {
    tripDate: values.tripDate,
    startTime: values.startTime,
    endTime: values.endTime,
    budget: Number(values.budget),
    startLocation: {
      label: values.originLabel,
      latitude,
      longitude,
    },
    travelPace: values.travelPace,
    environmentPreference: values.environmentPreference,
  };
}

function FieldError({ message }: { message?: string }) {
  return message ? (
    <p className="m-0 text-xs font-medium text-destructive">{message}</p>
  ) : null;
}

export function TripForm({
  initialTrip,
  submitLabel,
  onSubmit,
  onSearchLocations,
  onCancel,
}: TripFormProps) {
  const [values, setValues] = useState(() => initialValues(initialTrip));
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [locationState, setLocationState] = useState<
    "idle" | "locating" | "success" | "error"
  >("idle");
  const [locationMessage, setLocationMessage] = useState<string | null>(null);
  const [resolvedSource, setResolvedSource] = useState<
    "initial" | "search" | "gps" | null
  >(initialTrip ? "initial" : null);
  const [searchState, setSearchState] = useState<
    "idle" | "loading" | "success" | "error"
  >("idle");
  const [searchResults, setSearchResults] = useState<LocationSearchResult[]>([]);
  const [searchMessage, setSearchMessage] = useState<string | null>(null);

  function update<K extends keyof TripFormValues>(
    field: K,
    value: TripFormValues[K],
  ) {
    setValues((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => {
      const errorField =
        field === "originLabel"
          ? "startLocation.label"
          : field === "latitude"
            ? "startLocation.latitude"
            : field === "longitude"
              ? "startLocation.longitude"
              : field;

      if (!current[errorField]) return current;
      const next = { ...current };
      delete next[errorField];
      return next;
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);

    const hasCoordinates =
      values.latitude.trim() !== "" && values.longitude.trim() !== "";
    const latitude = hasCoordinates ? Number(values.latitude) : Number.NaN;
    const longitude = hasCoordinates ? Number(values.longitude) : Number.NaN;
    if (
      resolvedSource === null ||
      !hasCoordinates ||
      !Number.isFinite(latitude) ||
      !Number.isFinite(longitude) ||
      latitude < -90 ||
      latitude > 90 ||
      longitude < -180 ||
      longitude > 180
    ) {
      setLocationState("error");
      setLocationMessage(
        "Hãy tìm và chọn một địa điểm, hoặc dùng vị trí hiện tại trước khi lưu.",
      );
      return;
    }

    setPending(true);
    setFieldErrors({});

    try {
      await onSubmit(toRequest(values, latitude, longitude));
    } catch (error) {
      const feedback = getTripFormFeedback(error);
      setMessage(feedback.message);
      setFieldErrors(feedback.fieldErrors);
    } finally {
      setPending(false);
    }
  }

  const inputClassName =
    "h-11 rounded-[10px] border-border bg-background px-3 text-text-primary";

  function handleOriginChange(value: string) {
    setValues((current) => ({
      ...current,
      originLabel: value,
      latitude: "",
      longitude: "",
    }));
    setResolvedSource(null);
    setLocationState("idle");
    setLocationMessage(null);
    setSearchState("idle");
    setSearchResults([]);
    setSearchMessage(null);
    setFieldErrors((current) => {
      const next = { ...current };
      delete next["startLocation.label"];
      delete next["startLocation.latitude"];
      delete next["startLocation.longitude"];
      return next;
    });
  }

  async function runLocationSearch() {
    const query = values.originLabel.trim();
    if (!query) {
      setSearchState("error");
      setSearchResults([]);
      setSearchMessage("Nhập địa chỉ hoặc tên địa điểm để tìm.");
      return;
    }
    if (query.length > 120) {
      setSearchState("error");
      setSearchResults([]);
      setSearchMessage("Nội dung tìm kiếm không được dài quá 120 ký tự.");
      return;
    }

    setSearchState("loading");
    setSearchResults([]);
    setSearchMessage(null);
    setLocationMessage(null);

    try {
      const results = await onSearchLocations(query);
      setSearchResults(results);
      setSearchState("success");
      setSearchMessage(
        results.length === 0
          ? "Không tìm thấy địa điểm phù hợp. Hãy thử tên hoặc địa chỉ cụ thể hơn."
          : null,
      );
    } catch (error) {
      setSearchState("error");
      setSearchMessage(
        error instanceof ApiError && error.status === 0
          ? "Không thể kết nối đến máy chủ tìm kiếm. Hãy thử lại."
          : "Chưa thể tìm địa điểm lúc này. Hãy thử lại sau.",
      );
    }
  }

  function handleOriginKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== "Enter") return;
    event.preventDefault();
    if (searchState !== "loading" && !pending) {
      void runLocationSearch();
    }
  }

  function selectLocation(result: LocationSearchResult) {
    setValues((current) => ({
      ...current,
      originLabel: result.label,
      latitude: String(result.latitude),
      longitude: String(result.longitude),
    }));
    setResolvedSource("search");
    setSearchResults([]);
    setSearchState("idle");
    setSearchMessage(null);
    setLocationState("success");
    setLocationMessage("Đã chọn điểm xuất phát.");
    setFieldErrors((current) => {
      const next = { ...current };
      delete next["startLocation.label"];
      delete next["startLocation.latitude"];
      delete next["startLocation.longitude"];
      return next;
    });
  }

  function useCurrentLocation() {
    if (!("geolocation" in navigator)) {
      setLocationState("error");
      setLocationMessage("Trình duyệt này không hỗ trợ lấy vị trí hiện tại.");
      return;
    }

    setLocationState("locating");
    setLocationMessage(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setValues((current) => ({
          ...current,
          originLabel: "Vị trí hiện tại",
          latitude: position.coords.latitude.toFixed(7),
          longitude: position.coords.longitude.toFixed(7),
        }));
        setResolvedSource("gps");
        setSearchState("idle");
        setSearchResults([]);
        setSearchMessage(null);
        setFieldErrors((current) => {
          const next = { ...current };
          delete next["startLocation.latitude"];
          delete next["startLocation.longitude"];
          return next;
        });
        setLocationState("success");
        setLocationMessage("Đã dùng vị trí hiện tại cho điểm xuất phát.");
      },
      (error) => {
        setLocationState("error");
        setLocationMessage(
          error.code === error.PERMISSION_DENIED
            ? "Bạn chưa cho phép truy cập vị trí. Hãy cấp quyền rồi thử lại."
            : "Không thể lấy vị trí hiện tại. Hãy thử lại sau.",
        );
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 300000 },
    );
  }

  return (
    <form className="grid gap-0" onSubmit={handleSubmit}>
      {message ? (
        <Alert variant="destructive" role="alert">
          <AlertTitle>Chưa thể lưu chuyến đi</AlertTitle>
          <AlertDescription>{message}</AlertDescription>
        </Alert>
      ) : null}

      <section className="grid gap-5 border-b border-border py-6 first:pt-0">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Thời gian
          </p>
          <h2 className="mt-1 text-xl font-bold">Khung chuyến đi</h2>
          <p className="mt-1 mb-0 text-sm text-text-secondary">
            Chọn ngày, khoảng thời gian và ngân sách dự kiến.
          </p>
        </div>

        <div className="grid grid-cols-3 gap-4 max-md:grid-cols-1">
          <div className="grid gap-2">
            <Label htmlFor="trip-date" className="flex items-center gap-2">
              <span className="grid size-6 place-items-center rounded-md bg-primary-soft text-primary">
                <CalendarIcon className="size-3.5" />
              </span>
              Ngày đi
            </Label>
            <Input
              id="trip-date"
              className={inputClassName}
              type="date"
              value={values.tripDate}
              onChange={(event) => update("tripDate", event.target.value)}
              aria-invalid={Boolean(fieldErrors.tripDate)}
              required
              disabled={pending}
            />
            <FieldError message={fieldErrors.tripDate} />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="start-time" className="flex items-center gap-2">
              <span className="grid size-6 place-items-center rounded-md bg-primary-soft text-primary">
                <ClockIcon className="size-3.5" />
              </span>
              Bắt đầu
            </Label>
            <Input
              id="start-time"
              className={inputClassName}
              type="time"
              value={values.startTime}
              onChange={(event) => update("startTime", event.target.value)}
              aria-invalid={Boolean(fieldErrors.startTime)}
              required
              disabled={pending}
            />
            <FieldError message={fieldErrors.startTime} />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="end-time" className="flex items-center gap-2">
              <span className="grid size-6 place-items-center rounded-md bg-primary-soft text-primary">
                <ClockIcon className="size-3.5" />
              </span>
              Kết thúc
            </Label>
            <Input
              id="end-time"
              className={inputClassName}
              type="time"
              value={values.endTime}
              onChange={(event) => update("endTime", event.target.value)}
              aria-invalid={Boolean(fieldErrors.endTime)}
              required
              disabled={pending}
            />
            <FieldError message={fieldErrors.endTime} />
          </div>
        </div>

        <div className="grid gap-2">
          <Label htmlFor="trip-budget" className="flex items-center gap-2">
            <span className="grid size-6 place-items-center rounded-md bg-ochre-soft text-ochre-foreground">
              <WalletIcon className="size-3.5" />
            </span>
            Ngân sách dự kiến cho một người (VND)
          </Label>
          <Input
            id="trip-budget"
            className={inputClassName}
            type="number"
            min="0"
            max="100000000"
            step="0.01"
            value={values.budget}
            onChange={(event) => update("budget", event.target.value)}
            aria-invalid={Boolean(fieldErrors.budget)}
            placeholder="Ví dụ: 500000"
            required
            disabled={pending}
          />
          <FieldError message={fieldErrors.budget} />
        </div>
      </section>

      <section className="grid gap-5 border-b border-border py-6">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Điểm xuất phát
          </p>
          <h2 className="mt-1 text-xl font-bold">Bạn bắt đầu từ đâu?</h2>
          <p className="mt-1 mb-0 text-sm text-text-secondary">
            Nhập địa chỉ hoặc tên địa điểm. Bạn cũng có thể dùng vị trí hiện tại.
          </p>
        </div>

        <div className="grid gap-2">
          <Label htmlFor="origin-label" className="flex items-center gap-2">
            <span className="grid size-6 place-items-center rounded-md bg-primary-soft text-primary">
              <PinIcon className="size-3.5" />
            </span>
            Điểm xuất phát
          </Label>
          <Input
            id="origin-label"
            className={inputClassName}
            value={values.originLabel}
            onChange={(event) => handleOriginChange(event.target.value)}
            onKeyDown={handleOriginKeyDown}
            aria-invalid={Boolean(fieldErrors["startLocation.label"])}
            maxLength={255}
            placeholder="Nhập địa chỉ hoặc tên địa điểm..."
            required
            disabled={pending || searchState === "loading"}
          />
          <FieldError message={fieldErrors["startLocation.label"]} />
          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              size="sm"
              onClick={() => void runLocationSearch()}
              disabled={pending || searchState === "loading"}
            >
              <SearchIcon />
              {searchState === "loading" ? "Đang tìm…" : "Tìm"}
            </Button>
            {searchState === "loading" ? (
              <p className="m-0 text-sm text-text-secondary" role="status">
                Đang tìm tối đa 5 kết quả phù hợp…
              </p>
            ) : null}
          </div>
          {searchMessage ? (
            <p
              className={`m-0 text-sm ${searchState === "error" ? "text-destructive" : "text-text-secondary"}`}
              role={searchState === "error" ? "alert" : "status"}
            >
              {searchMessage}
            </p>
          ) : null}
          {searchResults.length > 0 ? (
            <div className="grid gap-2" aria-label="Kết quả tìm điểm xuất phát">
              <ul className="m-0 grid list-none gap-2 p-0">
                {searchResults.map((result) => (
                  <li
                    key={`${result.latitude}-${result.longitude}-${result.label}`}
                  >
                    <button
                      type="button"
                      className="w-full min-w-0 rounded-[10px] border border-border bg-surface px-3 py-2.5 text-left text-sm leading-5 text-text-primary transition-colors hover:border-primary/50 hover:bg-primary-soft focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/35 disabled:opacity-60"
                      onClick={() => selectLocation(result)}
                      disabled={pending}
                    >
                      <span className="block break-words font-medium">
                        {result.label}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
              <p className="m-0 text-xs text-text-secondary">
                Dữ liệu ©{" "}
                <a
                  className="underline underline-offset-2"
                  href="https://www.openstreetmap.org/copyright"
                  target="_blank"
                  rel="noreferrer"
                >
                  OpenStreetMap contributors
                </a>
              </p>
            </div>
          ) : null}
          {resolvedSource ? (
            <div className="flex min-w-0 items-start gap-2 rounded-[10px] border border-primary/25 bg-primary-soft px-3 py-2.5 text-sm text-primary-strong">
              <PinIcon className="mt-0.5 size-4 shrink-0" />
              <p className="m-0 min-w-0 break-words">
                <span className="font-semibold">Đã chọn:</span>{" "}
                {values.originLabel}
              </p>
            </div>
          ) : values.originLabel.trim() ? (
            <p className="m-0 text-xs text-text-secondary">
              Hãy tìm và chọn một kết quả để xác nhận điểm xuất phát.
            </p>
          ) : null}
          {resolvedSource === "search" ? (
            <p className="m-0 text-xs text-text-secondary">
              Dữ liệu ©{" "}
              <a
                className="underline underline-offset-2"
                href="https://www.openstreetmap.org/copyright"
                target="_blank"
                rel="noreferrer"
              >
                OpenStreetMap contributors
              </a>
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={useCurrentLocation}
            disabled={
              pending || locationState === "locating" || searchState === "loading"
            }
          >
            <PinIcon />
            {locationState === "locating" ? "Đang lấy vị trí…" : "Dùng vị trí của tôi"}
          </Button>
          {locationMessage ? (
            <p
              className={`m-0 text-sm ${locationState === "error" ? "text-destructive" : "text-primary-strong"}`}
              role={locationState === "error" ? "alert" : "status"}
            >
              {locationMessage}
            </p>
          ) : null}
        </div>
      </section>

      <section className="grid gap-5 border-b border-border py-6">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Phong cách
          </p>
          <h2 className="mt-1 text-xl font-bold">Sở thích chuyến đi</h2>
        </div>

        <div className="grid grid-cols-2 gap-4 max-md:grid-cols-1">
          <div className="grid gap-2">
            <Label htmlFor="travel-pace">Nhịp độ</Label>
            <Select
              value={values.travelPace}
              onValueChange={(value) =>
                update("travelPace", value as TravelPace)
              }
              disabled={pending}
            >
              <SelectTrigger
                id="travel-pace"
                className="h-11 w-full rounded-[10px] border-border bg-background"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent position="popper">
                {Object.entries(paceLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <FieldError message={fieldErrors.travelPace} />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="environment-preference">Không gian ưu tiên</Label>
            <Select
              value={values.environmentPreference}
              onValueChange={(value) =>
                update("environmentPreference", value as EnvironmentPreference)
              }
              disabled={pending}
            >
              <SelectTrigger
                id="environment-preference"
                className="h-11 w-full rounded-[10px] border-border bg-background"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent position="popper">
                {Object.entries(environmentLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <FieldError message={fieldErrors.environmentPreference} />
          </div>
        </div>

      </section>

      <footer className="flex justify-end gap-3 py-6 max-md:flex-col-reverse">
        {onCancel ? (
          <Button
            type="button"
            variant="outline"
            size="lg"
            onClick={onCancel}
            disabled={pending}
          >
            Hủy chỉnh sửa
          </Button>
        ) : null}
        <Button
          type="submit"
          variant="accent"
          size="lg"
          disabled={
            pending || searchState === "loading" || resolvedSource === null
          }
        >
          {pending ? "Đang lưu…" : submitLabel}
        </Button>
      </footer>
    </form>
  );
}
