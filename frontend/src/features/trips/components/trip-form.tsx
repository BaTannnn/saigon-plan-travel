"use client";

import { useState, type FormEvent } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  CalendarIcon,
  ClockIcon,
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
import {
  StartLocationField,
  type ResolvedLocationSource,
  type StartLocationValue,
} from "@/features/trips/components/start-location-field";
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
  const [resolvedSource, setResolvedSource] = useState<
    ResolvedLocationSource
  >(initialTrip ? "initial" : null);
  const [locationValidationMessage, setLocationValidationMessage] = useState<
    string | null
  >(null);
  const [locationSearching, setLocationSearching] = useState(false);

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
      setLocationValidationMessage(
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

  function handleLocationChange(location: StartLocationValue) {
    setValues((current) => ({
      ...current,
      originLabel: location.label,
      latitude: location.latitude,
      longitude: location.longitude,
    }));
    setResolvedSource(location.source);
    setLocationValidationMessage(null);
    setFieldErrors((current) => {
      const next = { ...current };
      delete next["startLocation.latitude"];
      delete next["startLocation.longitude"];
      if (location.source !== "gps") {
        delete next["startLocation.label"];
      }
      return next;
    });
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
          <h2 className="m-0 text-xl font-bold">Khung chuyến đi</h2>
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

      <StartLocationField
        value={{
          label: values.originLabel,
          latitude: values.latitude,
          longitude: values.longitude,
          source: resolvedSource,
        }}
        pending={pending}
        fieldError={fieldErrors["startLocation.label"]}
        validationMessage={locationValidationMessage}
        onChange={handleLocationChange}
        onClearValidationMessage={() => setLocationValidationMessage(null)}
        onSearchBusyChange={setLocationSearching}
        onSearchLocations={onSearchLocations}
      />

      <section className="grid gap-5 border-b border-border py-6">
        <div>
          <h2 className="m-0 text-xl font-bold">Sở thích chuyến đi</h2>
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
            pending || locationSearching || resolvedSource === null
          }
        >
          {pending ? "Đang lưu…" : submitLabel}
        </Button>
      </footer>
    </form>
  );
}
