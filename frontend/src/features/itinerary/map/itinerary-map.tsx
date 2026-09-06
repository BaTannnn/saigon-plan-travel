"use client";

import { useEffect, useMemo } from "react";
import Link from "next/link";
import L from "leaflet";
import {
  MapContainer,
  Marker,
  TileLayer,
  Tooltip,
  ZoomControl,
  useMap,
} from "react-leaflet";
import {
  GEOAPIFY_TILE_ATTRIBUTION,
  GEOAPIFY_TILE_MAX_ZOOM,
  GEOAPIFY_TILE_URL,
} from "@/lib/geoapify-map";
import { cn } from "@/lib/utils";
import type { ItineraryItemResponse } from "@/types/itinerary";
import styles from "./itinerary-map.module.css";

type Origin = {
  label: string;
  latitude: number;
  longitude: number;
};

type ItineraryMapProps = {
  origin: Origin;
  items: ItineraryItemResponse[];
  selectedItemPublicId: string | null;
  onSelectItem: (itemPublicId: string) => void;
};

const originIcon = L.divIcon({
  className: styles.markerWrapper,
  html: `<span class="${styles.originMarker}"></span>`,
  iconSize: [40, 48],
  iconAnchor: [20, 46],
  tooltipAnchor: [0, -42],
});

function itemIcon(sequenceNo: number, selected: boolean) {
  return L.divIcon({
    className: styles.markerWrapper,
    html: `<span class="${styles.itemMarker}${selected ? ` ${styles.itemMarkerSelected}` : ""}"><b>${sequenceNo}</b></span>`,
    iconSize: [42, 50],
    iconAnchor: [21, 48],
    tooltipAnchor: [0, -44],
  });
}

function MapViewport({
  origin,
  items,
  selectedItemPublicId,
}: Pick<ItineraryMapProps, "origin" | "items" | "selectedItemPublicId">) {
  const map = useMap();

  useEffect(() => {
    const coordinates: [number, number][] = [
      [origin.latitude, origin.longitude],
      ...items.map(
        (item) =>
          [item.place.latitude, item.place.longitude] as [number, number],
      ),
    ];

    if (items.length === 0) {
      map.setView([origin.latitude, origin.longitude], 15);
      return;
    }

    map.fitBounds(coordinates, { padding: [56, 56], maxZoom: 15 });
  }, [items, map, origin.latitude, origin.longitude]);

  useEffect(() => {
    if (!selectedItemPublicId) return;

    const selected = items.find(
      (item) => item.publicId === selectedItemPublicId,
    );
    if (!selected) return;

    map.flyTo(
      [selected.place.latitude, selected.place.longitude],
      Math.max(map.getZoom(), 15),
      { duration: 0.6 },
    );
  }, [items, map, selectedItemPublicId]);

  return null;
}

function hasValidCoordinates(item: ItineraryItemResponse) {
  const { latitude, longitude } = item.place;

  return (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180
  );
}

export function ItineraryMap({
  origin,
  items,
  selectedItemPublicId,
  onSelectItem,
}: ItineraryMapProps) {
  const validItems = useMemo(() => items.filter(hasValidCoordinates), [items]);
  const selectedItem = useMemo(
    () =>
      validItems.find((item) => item.publicId === selectedItemPublicId) ?? null,
    [validItems, selectedItemPublicId],
  );

  return (
    <div className={cn(styles.root, "relative size-full")}>
      <MapContainer
        className="size-full"
        center={[origin.latitude, origin.longitude]}
        zoom={14}
        scrollWheelZoom
        zoomControl={false}
      >
        <TileLayer
          attribution={GEOAPIFY_TILE_ATTRIBUTION}
          maxZoom={GEOAPIFY_TILE_MAX_ZOOM}
          url={GEOAPIFY_TILE_URL}
        />
        <MapViewport
          origin={origin}
          items={validItems}
          selectedItemPublicId={selectedItemPublicId}
        />
        <ZoomControl position="bottomright" />

        <Marker
          position={[origin.latitude, origin.longitude]}
          icon={originIcon}
          title={origin.label}
        >
          <Tooltip permanent direction="top" opacity={1}>
            Điểm xuất phát · {origin.label}
          </Tooltip>
        </Marker>

        {validItems.map((item) => (
          <Marker
            key={item.publicId}
            position={[item.place.latitude, item.place.longitude]}
            icon={itemIcon(
              item.sequenceNo,
              item.publicId === selectedItemPublicId,
            )}
            title={item.place.name}
            eventHandlers={{
              click: () => onSelectItem(item.publicId),
            }}
          >
            <Tooltip direction="top" opacity={1}>
              {item.sequenceNo}. {item.place.name}
            </Tooltip>
          </Marker>
        ))}
      </MapContainer>

      {selectedItem ? (
        <div
          className="absolute bottom-7 left-1/2 z-[500] grid min-w-[min(330px,calc(100%_-_100px))] -translate-x-1/2 rounded-mint-md border border-border/50 bg-card px-[18px] py-3.5 shadow-mint-sm max-md:bottom-[90px] max-md:min-w-[calc(100%_-_32px)]"
          aria-live="polite"
        >
          <strong>
            {selectedItem.sequenceNo}. {selectedItem.place.name}
          </strong>
          <Link
            className="mt-1.5 text-[0.78rem] font-extrabold text-primary"
            href={`/places/${selectedItem.place.slug}`}
          >
            Xem chi tiết
          </Link>
        </div>
      ) : null}
    </div>
  );
}
