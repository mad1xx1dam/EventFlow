export type EventStatus = "ACTIVE" | "CANCELLED";

export interface EventRequest {
  title: string;
  description?: string | null;
  startsAt: string;
  address: string;
  lat?: number | null;
  lon?: number | null;
}

export interface EventResponse {
  id: number;
  creatorId: number;
  title: string;
  description: string | null;
  startsAt: string;
  address: string;
  lat: number | null;
  lon: number | null;
  posterPath: string | null;
  posterUrl: string | null;
  status: EventStatus;
  createdAt: string;
  updatedAt: string;
}

export interface FileUploadResponse {
  objectName: string;
  objectPath: string;
  objectUrl: string;
  bucket: string;
}