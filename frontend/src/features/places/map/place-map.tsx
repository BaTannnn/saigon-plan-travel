"use client";

import { useEffect, useMemo } from "react";
import Link from "next/link";
import L, { type LatLngBoundsExpression } from "leaflet";
import { Button } from "@/components/ui/button";
import {
  MapContainer,
  Marker,
  TileLayer,
  ZoomControl,
  useMap,
  useMapEvents,
} from "react-leaflet";
import { PinIcon } from "@/components/ui/icons";
import { cn } from "@/lib/utils";
import type { PlaceSummary } from "@/types/place";
import styles from "./place-map.module.css";

const SAIGON_CENTER: [number, number] = [10.7769, 106.7009];

type PlaceMapProps = {
  places: PlaceSummary[];
  selectedSlug?: string | null;
  onSelect?: (slug: string) => void;
  detailMode?: boolean;
};

function markerIcon(index: number, selected: boolean) {
  return L.divIcon({
    className: styles.markerWrapper,
    html: `<span class="${styles.marker}${selected ? ` ${styles.markerSelected}` : ""}"><b>${index + 1}</b></span>`,
    iconSize: [42, 50],
    iconAnchor: [21, 48],
  });
}

function MapViewport({
  places,
  selectedSlug,
}: Pick<PlaceMapProps, "places" | "selectedSlug">) {
  const map = useMap();

  useEffect(() => {
    if (places.length === 0) return;
    if (places.length === 1) {
      map.setView([places[0].latitude, places[0].longitude], 15);
      return;
    }

    const bounds: LatLngBoundsExpression = places.map((place) => [
      place.latitude,
      place.longitude,
    ]);
    map.fitBounds(bounds, { padding: [48, 48], maxZoom: 15 });
  }, [map, places]);

  useEffect(() => {
    const selected = places.find((place) => place.slug === selectedSlug);
    if (selected) {
      map.flyTo(
        [selected.latitude, selected.longitude],
        Math.max(map.getZoom(), 15),
        {
          duration: 0.6,
        },
      );
    }
  }, [map, places, selectedSlug]);

  return null;
}

function LocationControl() {
  const map = useMapEvents({
    locationfound(event) {
      map.flyTo(event.latlng, 15);
      L.circleMarker(event.latlng, {
        radius: 7,
        color: "var(--surface)",
        weight: 3,
        fillColor: "var(--primary)",
        fillOpacity: 1,
      }).addTo(map);
    },
  });

  return (
    <Button
      type="button"
      className="absolute right-3 bottom-[82px] z-[800] size-11 rounded-md border-2 border-black/20 bg-surface text-primary-strong"
      onClick={() => map.locate({ setView: false })}
      aria-label="Định vị vị trí của tôi"
      title="Vị trí của tôi"
      size="icon"
      variant="outline"
    >
      <PinIcon />
    </Button>
  );
}

export function PlaceMap({
  places,
  selectedSlug = null,
  onSelect,
  detailMode = false,
}: PlaceMapProps) {
  const selectedPlace = useMemo(
    () => places.find((place) => place.slug === selectedSlug) ?? null,
    [places, selectedSlug],
  );

  return (
    <div className={cn(styles.root, "relative size-full")}>
      <MapContainer
        className="size-full"
        center={
          places[0] ? [places[0].latitude, places[0].longitude] : SAIGON_CENTER
        }
        zoom={13}
        scrollWheelZoom
        zoomControl={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapViewport places={places} selectedSlug={selectedSlug} />
        <ZoomControl position="bottomright" />
        <LocationControl />
        {places.map((place, index) => (
          <Marker
            key={place.id}
            position={[place.latitude, place.longitude]}
            icon={markerIcon(index, place.slug === selectedSlug)}
            eventHandlers={{ click: () => onSelect?.(place.slug) }}
            title={place.name}
          />
        ))}
      </MapContainer>

      {!detailMode && selectedPlace ? (
        <div
          className="absolute bottom-7 left-1/2 z-[500] grid min-w-[min(320px,calc(100%_-_100px))] -translate-x-1/2 rounded-mint-md border border-border bg-surface px-[18px] py-3.5 shadow-mint-md max-md:bottom-[90px] max-md:min-w-[calc(100%_-_32px)]"
          aria-live="polite"
        >
          <p className="mb-0.5 text-xs text-text-secondary">
            {selectedPlace.administrativeUnitName ?? "Chưa xác định"}
          </p>
          <strong>{selectedPlace.name}</strong>
          <Link
            className="mt-1.5 text-[0.78rem] font-extrabold text-primary"
            href={`/places/${selectedPlace.slug}`}
          >
            Xem chi tiết
          </Link>
        </div>
      ) : null}
    </div>
  );
}
