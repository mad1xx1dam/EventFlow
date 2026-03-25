import axiosInstance from "./axios";
import type { EventRequest, EventResponse } from "../types/event";
import type { InvitationResponse } from "../types/invitation";

const buildEventFormData = (event: EventRequest, poster?: File | null): FormData => {
  const formData = new FormData();

  formData.append(
    "event",
    new Blob([JSON.stringify(event)], {
      type: "application/json",
    })
  );

  if (poster) {
    formData.append("poster", poster);
  }

  return formData;
};

export const eventsApi = {
  async createEvent(event: EventRequest, poster?: File | null): Promise<EventResponse> {
    const formData = buildEventFormData(event, poster);

    const { data } = await axiosInstance.post<EventResponse>("/events", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });

    return data;
  },

  async getEventById(eventId: number): Promise<EventResponse> {
    const { data } = await axiosInstance.get<EventResponse>(`/events/${eventId}`);
    return data;
  },

  async getMyEvents(): Promise<EventResponse[]> {
    const { data } = await axiosInstance.get<EventResponse[]>("/events/my");
    return data;
  },

  async updateEvent(
    eventId: number,
    event: EventRequest,
    poster?: File | null
  ): Promise<EventResponse> {
    const formData = buildEventFormData(event, poster);

    const { data } = await axiosInstance.put<EventResponse>(`/events/${eventId}`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });

    return data;
  },

  async cancelEvent(eventId: number): Promise<EventResponse> {
    const { data } = await axiosInstance.delete<EventResponse>(`/events/${eventId}`);
    return data;
  },

  async getEventGuests(eventId: number): Promise<InvitationResponse[]> {
    const { data } = await axiosInstance.get<InvitationResponse[]>(`/events/${eventId}/guests`);
    return data;
  },
};

export default eventsApi;