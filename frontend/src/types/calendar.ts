export interface CalendarEventItemResponse {
  eventId: number;
  title: string;
  startsAt: string;
  address: string;
  colorType: string;
  guestToken?: string;
}

export interface DashboardSummaryResponse {
  createdEventsCount: number;
  acceptedInvitationsCount: number;
}

export interface CalendarResponse {
  year: number;
  month: number;
  creatorEvents: CalendarEventItemResponse[];
  guestEvents: CalendarEventItemResponse[];
}