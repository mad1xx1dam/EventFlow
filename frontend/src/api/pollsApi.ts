import axiosInstance from "./axios";
import type { CreatePollRequest, PollResponse, VotePollRequest } from "../types/poll";

export const pollsApi = {
  async createPoll(eventId: number, payload: CreatePollRequest): Promise<PollResponse> {
    const { data } = await axiosInstance.post<PollResponse>(`/events/${eventId}/polls`, payload);
    return data;
  },

  async getActivePoll(eventId: number): Promise<PollResponse> {
    const { data } = await axiosInstance.get<PollResponse>(`/events/${eventId}/polls/active`);
    return data;
  },

  async votePoll(
    pollId: number,
    guestToken: string,
    payload: VotePollRequest
  ): Promise<PollResponse> {
    const { data } = await axiosInstance.post<PollResponse>(`/polls/${pollId}/vote`, payload, {
      params: { guestToken },
    });
    return data;
  },

  async closePoll(pollId: number): Promise<PollResponse> {
    const { data } = await axiosInstance.post<PollResponse>(`/polls/${pollId}/close`);
    return data;
  },

  async getPollResults(pollId: number): Promise<PollResponse> {
    const { data } = await axiosInstance.get<PollResponse>(`/polls/${pollId}/results`);
    return data;
  },
};

export default pollsApi;