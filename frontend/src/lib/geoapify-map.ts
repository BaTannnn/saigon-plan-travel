const geoapifyApiKey = process.env.NEXT_PUBLIC_GEOAPIFY_API_KEY ?? "";

export const GEOAPIFY_TILE_URL =
  "https://maps.geoapify.com/v1/tile/osm-carto/{z}/{x}/{y}.png" +
  `?apiKey=${encodeURIComponent(geoapifyApiKey)}`;

export const GEOAPIFY_TILE_ATTRIBUTION =
  'Powered by <a href="https://www.geoapify.com/" target="_blank">Geoapify</a> | <a href="https://www.openstreetmap.org/copyright" target="_blank">© OpenStreetMap contributors</a>';

export const GEOAPIFY_TILE_MAX_ZOOM = 20;
