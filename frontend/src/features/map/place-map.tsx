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
import type { PlaceSummary } from "@/types/place";

const SAIGON_CENTER: [number, number] = [10.7769, 106.7009];

type PlaceMapProps = {
  places: PlaceSummary[];
  selectedSlug?: string | null;
  onSelect?: (slug: string) => void;
  detailMode?: boolean;
};

function markerIcon(index: number, selected: boolean) {
  return L.divIcon({
    className: "custom-map-marker-wrapper",
    html: `<span class="custom-map-marker${selected ? " selected" : ""}"><b>${index + 1}</b></span>`,
    iconSize: [42, 50],
    iconAnchor: [21, 48],
  });
}

function MapViewport({ places, selectedSlug }: Pick<PlaceMapProps, "places" | "selectedSlug">) {
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
      map.flyTo([selected.latitude, selected.longitude], Math.max(map.getZoom(), 15), {
        duration: 0.6,
      });
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
      className="location-control"
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
    <div className={detailMode ? "place-map detail-map" : "place-map"}>
      <MapContainer
        center={places[0] ? [places[0].latitude, places[0].longitude] : SAIGON_CENTER}
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
        <div className="map-preview" aria-live="polite">
          <p>{selectedPlace.district}</p>
          <strong>{selectedPlace.name}</strong>
          <Link href={`/places/${selectedPlace.slug}`}>Xem chi tiết</Link>
        </div>
      ) : null}
    </div>
  );
}
