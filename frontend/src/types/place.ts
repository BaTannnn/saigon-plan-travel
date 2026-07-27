export type AdministrativeUnitType = "WARD" | "COMMUNE" | "SPECIAL_ZONE";

export type PlaceSummary = {
  id: number;
  name: string;
  slug: string;
  shortDescription: string | null;
  administrativeUnitName: string | null;
  administrativeUnitType: AdministrativeUnitType | null;
  latitude: number;
  longitude: number;
  estimatedVisitMinutes: number;
  minCost: number;
  maxCost: number;
  indoor: boolean;
};

export type PlacePage = {
  content: PlaceSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type Category = {
  id: number;
  name: string;
  slug: string;
};

export type OpeningHour = {
  dayOfWeek: number;
  closed: boolean;
  openTime: string | null;
  closeTime: string | null;
};

export type PlaceDetail = PlaceSummary & {
  fullDescription: string | null;
  address: string;
  categories: Category[];
  openingHours: OpeningHour[];
};

export type PlacesSearchFilters = {
  keyword?: string;
  administrativeUnitName?: string;
  category?: string;
  indoor?: "true" | "false";
  maxCost?: string;
  page: number;
  size: number;
};

export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiProblem = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  fieldErrors?: ApiFieldError[];
};
