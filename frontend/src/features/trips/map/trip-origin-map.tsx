"use client";

import { useEffect } from "react";
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
import styles from "./trip-origin-map.module.css";

type TripOriginMapProps = {
  latitude: number;
  longitude: number;
  label: string;
};

const originIcon = L.divIcon({
  className: styles.markerWrapper,
  html: `<span class="${styles.marker}"><span class="${styles.markerCenter}"></span></span>`,
  iconSize: [42, 50],
  iconAnchor: [21, 48],
  tooltipAnchor: [0, -44],
});

function OriginViewport({
  latitude,
  longitude,
}: Omit<TripOriginMapProps, "label">) {
  const map = useMap();

  useEffect(() => {
    map.setView([latitude, longitude], Math.max(map.getZoom(), 15));
  }, [latitude, longitude, map]);

  return null;
}

export function TripOriginMap({
  latitude,
  longitude,
  label,
}: TripOriginMapProps) {
  return (
    <div className={cn(styles.root, "relative size-full")}>
      <MapContainer
        className="size-full"
        center={[latitude, longitude]}
        zoom={15}
        scrollWheelZoom
        zoomControl={false}
      >
        <TileLayer
          attribution={GEOAPIFY_TILE_ATTRIBUTION}
          maxZoom={GEOAPIFY_TILE_MAX_ZOOM}
          url={GEOAPIFY_TILE_URL}
        />
        <OriginViewport latitude={latitude} longitude={longitude} />
        <ZoomControl position="bottomright" />
        <Marker
          position={[latitude, longitude]}
          icon={originIcon}
          title={label}
        >
          <Tooltip permanent direction="top" opacity={1}>
            {label}
          </Tooltip>
        </Marker>
      </MapContainer>
    </div>
  );
}
