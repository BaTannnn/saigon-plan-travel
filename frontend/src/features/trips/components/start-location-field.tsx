"use client";

import { useState, type KeyboardEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PinIcon, SearchIcon } from "@/components/ui/icons";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/api-client";
import type { LocationSearchResult } from "@/types/location";

export type ResolvedLocationSource = "initial" | "search" | "gps" | null;

export type StartLocationValue = {
  label: string;
  latitude: string;
  longitude: string;
  source: ResolvedLocationSource;
};

type StartLocationFieldProps = {
  value: StartLocationValue;
  pending: boolean;
  fieldError?: string;
  validationMessage: string | null;
  onChange: (value: StartLocationValue) => void;
  onClearValidationMessage: () => void;
  onSearchBusyChange: (busy: boolean) => void;
  onSearchLocations: (query: string) => Promise<LocationSearchResult[]>;
};

type LocationState = "idle" | "locating" | "success" | "error";
type SearchState = "idle" | "loading" | "success" | "error";

export function StartLocationField({
  value,
  pending,
  fieldError,
  validationMessage,
  onChange,
  onClearValidationMessage,
  onSearchBusyChange,
  onSearchLocations,
}: StartLocationFieldProps) {
  const [locationState, setLocationState] =
    useState<LocationState>("idle");
  const [locationMessage, setLocationMessage] = useState<string | null>(null);
  const [searchState, setSearchState] = useState<SearchState>("idle");
  const [searchResults, setSearchResults] = useState<LocationSearchResult[]>(
    [],
  );
  const [searchMessage, setSearchMessage] = useState<string | null>(null);

  function handleOriginChange(label: string) {
    onChange({ label, latitude: "", longitude: "", source: null });
    setLocationState("idle");
    setLocationMessage(null);
    setSearchState("idle");
    setSearchResults([]);
    setSearchMessage(null);
  }

  async function runLocationSearch() {
    const query = value.label.trim();
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
    onClearValidationMessage();
    onSearchBusyChange(true);

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
    } finally {
      onSearchBusyChange(false);
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
    onChange({
      label: result.label,
      latitude: String(result.latitude),
      longitude: String(result.longitude),
      source: "search",
    });
    setSearchResults([]);
    setSearchState("idle");
    setSearchMessage(null);
    setLocationState("success");
    setLocationMessage("Đã chọn điểm xuất phát.");
  }

  function useCurrentLocation() {
    onClearValidationMessage();

    if (!("geolocation" in navigator)) {
      setLocationState("error");
      setLocationMessage("Trình duyệt này không hỗ trợ lấy vị trí hiện tại.");
      return;
    }

    setLocationState("locating");
    setLocationMessage(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        onChange({
          label: "Vị trí hiện tại",
          latitude: position.coords.latitude.toFixed(7),
          longitude: position.coords.longitude.toFixed(7),
          source: "gps",
        });
        setSearchState("idle");
        setSearchResults([]);
        setSearchMessage(null);
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

  const inputClassName =
    "h-11 rounded-[10px] border-border bg-background px-3 text-text-primary";
  const resolvedLocationMessage = validationMessage ?? locationMessage;
  const resolvedLocationState = validationMessage ? "error" : locationState;

  return (
    <section className="grid gap-5 border-b border-border py-6">
      <div>
        <h2 className="m-0 text-xl font-bold">Bạn bắt đầu từ đâu?</h2>
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
          value={value.label}
          onChange={(event) => handleOriginChange(event.target.value)}
          onKeyDown={handleOriginKeyDown}
          aria-invalid={Boolean(fieldError)}
          maxLength={255}
          placeholder="Nhập địa chỉ hoặc tên địa điểm..."
          required
          disabled={pending || searchState === "loading"}
        />
        {fieldError ? (
          <p className="m-0 text-xs font-medium text-destructive">
            {fieldError}
          </p>
        ) : null}
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
          <Button
            type="button"
            variant="outline"
            onClick={useCurrentLocation}
            disabled={
              pending ||
              locationState === "locating" ||
              searchState === "loading"
            }
          >
            <PinIcon />
            {locationState === "locating"
              ? "Đang lấy vị trí…"
              : "Dùng vị trí của tôi"}
          </Button>
          {searchState === "loading" ? (
            <p className="m-0 text-sm text-text-secondary" role="status">
              Đang tìm tối đa 5 kết quả phù hợp…
            </p>
          ) : null}
        </div>
        {resolvedLocationMessage ? (
          <p
            className={`m-0 text-sm ${resolvedLocationState === "error" ? "text-destructive" : "text-primary-strong"}`}
            role={resolvedLocationState === "error" ? "alert" : "status"}
          >
            {resolvedLocationMessage}
          </p>
        ) : null}
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
            <LocationAttribution />
          </div>
        ) : null}
        {value.source ? (
          <div className="flex min-w-0 items-start gap-2 rounded-[10px] border border-primary/25 bg-primary-soft px-3 py-2.5 text-sm text-primary-strong">
            <PinIcon className="mt-0.5 size-4 shrink-0" />
            <p className="m-0 min-w-0 break-words">
              <span className="font-semibold">Đã chọn:</span> {value.label}
            </p>
          </div>
        ) : value.label.trim() ? (
          <p className="m-0 text-xs text-text-secondary">
            Hãy tìm và chọn một kết quả để xác nhận điểm xuất phát.
          </p>
        ) : null}
        {value.source === "search" ? <LocationAttribution /> : null}
      </div>
    </section>
  );
}

function LocationAttribution() {
  return (
    <p className="m-0 text-xs text-text-secondary">
      Powered by{" "}
      <a
        className="underline underline-offset-2"
        href="https://www.geoapify.com/"
        target="_blank"
        rel="noreferrer"
      >
        Geoapify
      </a>{" "}
      |{" "}
      <a
        className="underline underline-offset-2"
        href="https://www.openstreetmap.org/copyright"
        target="_blank"
        rel="noreferrer"
      >
        © OpenStreetMap contributors
      </a>
    </p>
  );
}
