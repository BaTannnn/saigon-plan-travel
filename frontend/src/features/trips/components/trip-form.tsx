"use client";

import { useState, type FormEvent } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { getTripFormFeedback } from "@/features/trips/trip-errors";
import type { Category } from "@/types/category";
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
  categorySlugs: string[];
};

type TripFormProps = {
  categories: Category[];
  initialTrip?: TripResponse;
  submitLabel: string;
  onSubmit: (request: SaveTripRequest) => Promise<void>;
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
      categorySlugs: [],
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
    categorySlugs: initialTrip.categoryPreferences.map((item) => item.slug),
  };
}

function toRequest(values: TripFormValues): SaveTripRequest {
  return {
    tripDate: values.tripDate,
    startTime: values.startTime,
    endTime: values.endTime,
    budget: Number(values.budget),
    startLocation: {
      label: values.originLabel,
      latitude: Number(values.latitude),
      longitude: Number(values.longitude),
    },
    travelPace: values.travelPace,
    environmentPreference: values.environmentPreference,
    categorySlugs: values.categorySlugs,
  };
}

function FieldError({ message }: { message?: string }) {
  return message ? (
    <p className="m-0 text-xs font-medium text-destructive">{message}</p>
  ) : null;
}

export function TripForm({
  categories,
  initialTrip,
  submitLabel,
  onSubmit,
  onCancel,
}: TripFormProps) {
  const [values, setValues] = useState(() => initialValues(initialTrip));
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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

  function toggleCategory(slug: string) {
    const selected = values.categorySlugs.includes(slug);
    if (!selected && values.categorySlugs.length >= 5) return;

    update(
      "categorySlugs",
      selected
        ? values.categorySlugs.filter((item) => item !== slug)
        : [...values.categorySlugs, slug],
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);

    if (values.categorySlugs.length === 0) {
      setFieldErrors({
        categorySlugs: "Hãy chọn ít nhất một danh mục.",
      });
      return;
    }

    setPending(true);
    setFieldErrors({});

    try {
      await onSubmit(toRequest(values));
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

  return (
    <form className="grid gap-5" onSubmit={handleSubmit}>
      {message ? (
        <Alert variant="destructive" role="alert">
          <AlertTitle>Chưa thể lưu chuyến đi</AlertTitle>
          <AlertDescription>{message}</AlertDescription>
        </Alert>
      ) : null}

      <Card className="grid gap-5 rounded-mint-md border-border bg-surface p-5 ring-0 max-md:p-4">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Thời gian và ngân sách
          </p>
          <h2 className="mt-1 text-xl font-bold">Khung chuyến đi</h2>
        </div>

        <div className="grid grid-cols-3 gap-4 max-md:grid-cols-1">
          <div className="grid gap-2">
            <Label htmlFor="trip-date">Ngày đi</Label>
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
            <Label htmlFor="start-time">Bắt đầu</Label>
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
            <Label htmlFor="end-time">Kết thúc</Label>
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
          <Label htmlFor="trip-budget">
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
      </Card>

      <Card className="grid gap-5 rounded-mint-md border-border bg-surface p-5 ring-0 max-md:p-4">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Điểm xuất phát
          </p>
          <h2 className="mt-1 text-xl font-bold">Vị trí bắt đầu</h2>
          <p className="mt-1 mb-0 text-sm text-text-secondary">
            Nhập nhãn dễ nhớ và tọa độ. Tìm kiếm địa chỉ hoặc GPS sẽ
            thuộc một lát cắt sau.
          </p>
        </div>

        <div className="grid gap-2">
          <Label htmlFor="origin-label">Tên điểm xuất phát</Label>
          <Input
            id="origin-label"
            className={inputClassName}
            value={values.originLabel}
            onChange={(event) => update("originLabel", event.target.value)}
            aria-invalid={Boolean(fieldErrors["startLocation.label"])}
            maxLength={255}
            placeholder="Ví dụ: Chợ Bến Thành"
            required
            disabled={pending}
          />
          <FieldError message={fieldErrors["startLocation.label"]} />
        </div>

        <div className="grid grid-cols-2 gap-4 max-md:grid-cols-1">
          <div className="grid gap-2">
            <Label htmlFor="origin-latitude">Vĩ độ</Label>
            <Input
              id="origin-latitude"
              className={inputClassName}
              type="number"
              min="-90"
              max="90"
              step="0.0000001"
              value={values.latitude}
              onChange={(event) => update("latitude", event.target.value)}
              aria-invalid={Boolean(fieldErrors["startLocation.latitude"])}
              placeholder="10.7726400"
              required
              disabled={pending}
            />
            <FieldError message={fieldErrors["startLocation.latitude"]} />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="origin-longitude">Kinh độ</Label>
            <Input
              id="origin-longitude"
              className={inputClassName}
              type="number"
              min="-180"
              max="180"
              step="0.0000001"
              value={values.longitude}
              onChange={(event) => update("longitude", event.target.value)}
              aria-invalid={Boolean(fieldErrors["startLocation.longitude"])}
              placeholder="106.6980500"
              required
              disabled={pending}
            />
            <FieldError message={fieldErrors["startLocation.longitude"]} />
          </div>
        </div>
      </Card>

      <Card className="grid gap-5 rounded-mint-md border-border bg-surface p-5 ring-0 max-md:p-4">
        <div>
          <p className="m-0 text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
            Phong cách
          </p>
          <h2 className="mt-1 text-xl font-bold">Sở thích hành trình</h2>
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

        <fieldset className="grid gap-3">
          <div>
            <legend className="text-sm font-medium">Danh mục yêu thích</legend>
            <p className="mt-1 mb-0 text-xs text-text-secondary">
              Chọn từ 1 đến 5 danh mục. Thứ tự chọn không biểu thị ưu tiên.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {categories.map((category) => {
              const selected = values.categorySlugs.includes(category.slug);
              const disabled =
                pending || (!selected && values.categorySlugs.length >= 5);

              return (
                <label
                  key={category.id}
                  className={
                    disabled
                      ? "cursor-not-allowed opacity-55"
                      : "cursor-pointer"
                  }
                >
                  <input
                    className="peer sr-only"
                    type="checkbox"
                    checked={selected}
                    onChange={() => toggleCategory(category.slug)}
                    disabled={disabled}
                  />
                  <Badge
                    className="h-10 rounded-full border-border bg-background px-3.5 font-bold text-primary-strong peer-focus-visible:ring-3 peer-focus-visible:ring-ring/50 peer-checked:border-primary peer-checked:bg-primary-soft"
                    variant="outline"
                  >
                    {category.name}
                  </Badge>
                </label>
              );
            })}
          </div>
          <FieldError message={fieldErrors.categorySlugs} />
        </fieldset>
      </Card>

      <div className="flex justify-end gap-3 max-md:flex-col-reverse">
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
        <Button type="submit" size="lg" disabled={pending}>
          {pending ? "Đang lưu…" : submitLabel}
        </Button>
      </div>
    </form>
  );
}
