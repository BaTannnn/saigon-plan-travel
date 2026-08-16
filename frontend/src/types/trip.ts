export type TravelPace = "RELAXED" | "BALANCED" | "FAST";

export type EnvironmentPreference = "INDOOR" | "OUTDOOR" | "MIXED";

export type StartLocationRequest = {
  label: string;
  latitude: number;
  longitude: number;
};

export type SaveTripRequest = {
  tripDate: string;
  startTime: string;
  endTime: string;
  budget: number;
  startLocation: StartLocationRequest;
  travelPace: TravelPace;
  environmentPreference: EnvironmentPreference;
};

export type TripResponse = {
  publicId: string;
  tripDate: string;
  startTime: string;
  endTime: string;
  budget: number;
  startLocation: StartLocationRequest;
  travelPace: TravelPace;
  environmentPreference: EnvironmentPreference;
  createdAt: string;
  updatedAt: string;
};

export type TripSummaryResponse = {
  publicId: string;
  tripDate: string;
  startTime: string;
  endTime: string;
  budget: number;
  startLocationLabel: string;
  travelPace: TravelPace;
  environmentPreference: EnvironmentPreference;
  updatedAt: string;
};
