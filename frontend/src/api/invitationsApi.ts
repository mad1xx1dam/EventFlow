import axiosInstance from "./axios";
import type {
  CreateInvitationsRequest,
  GuestInvitationDetailsResponse,
  InvitationResponse,
  UpdateRsvpRequest,
} from "../types/invitation";

export const invitationsApi = {
  async createInvitations(
    eventId: number,
    payload: CreateInvitationsRequest
  ): Promise<InvitationResponse[]> {
    const { data } = await axiosInstance.post<InvitationResponse[]>(
      `/events/${eventId}/invitations`,
      payload
    );
    return data;
  },

  async resendInvitations(eventId: number): Promise<InvitationResponse[]> {
    const { data } = await axiosInstance.post<InvitationResponse[]>(
      `/events/${eventId}/invitations/resend`
    );
    return data;
  },

  async getGuestInvitation(
    eventId: number,
    guestToken: string
  ): Promise<GuestInvitationDetailsResponse> {
    const { data } = await axiosInstance.get<GuestInvitationDetailsResponse>(
      `/events/${eventId}/invite/${guestToken}`
    );
    return data;
  },

  async updateRsvp(
    eventId: number,
    guestToken: string,
    payload: UpdateRsvpRequest
  ): Promise<GuestInvitationDetailsResponse> {
    const { data } = await axiosInstance.post<GuestInvitationDetailsResponse>(
      `/events/${eventId}/invite/${guestToken}/rsvp`,
      payload
    );
    return data;
  },
};

export default invitationsApi;