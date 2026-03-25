export type CalendarColorType = string;

export interface CalendarEventItemResponse {
  eventId: number;
  title: string;
  startsAt: string;
  address: string;
  colorType: CalendarColorType;
}

export interface CalendarResponse {
  year: number;
  month: number;
  creatorEvents: CalendarEventItemResponse[];
  guestEvents: CalendarEventItemResponse[];
}

export interface DashboardSummaryResponse {
  createdEventsCount: number;
  acceptedInvitationsCount: number;
}