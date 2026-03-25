export type RsvpStatus = "PENDING" | "GOING" | "MAYBE" | "DECLINED";

export interface CreateInvitationsRequest {
  guestEmails: string[];
}

export interface UpdateRsvpRequest {
  rsvpStatus: Exclude<RsvpStatus, "PENDING"> | RsvpStatus;
}

export interface InvitationResponse {
  id: number;
  eventId: number;
  registeredUserId: number | null;
  guestEmail: string;
  guestToken: string;
  rsvpStatus: RsvpStatus;
  tokenActive: boolean;
  invitedAt: string;
  respondedAt: string | null;
  invitationUrl: string;
}

export interface GuestInvitationDetailsResponse {
  eventId: number;
  title: string;
  description: string | null;
  startsAt: string;
  address: string;
  lat: number | null;
  lon: number | null;
  posterUrl: string | null;
  guestEmail: string;
  rsvpStatus: RsvpStatus;
}

export interface RsvpCountersResponse {
  eventId: number;
  goingCount: number;
  maybeCount: number;
  declinedCount: number;
  pendingCount: number;
}