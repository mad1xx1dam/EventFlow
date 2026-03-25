export type PollStatus = "ACTIVE" | "CLOSED";

export interface CreatePollRequest {
  question: string;
  options: string[];
}

export interface VotePollRequest {
  pollOptionId: number;
}

export interface PollOptionResponse {
  id: number;
  optionText: string;
  position: number;
  votesCount: number;
}

export interface PollResponse {
  id: number;
  eventId: number;
  createdByUserId: number;
  question: string;
  status: PollStatus;
  startedAt: string;
  closedAt: string | null;
  options: PollOptionResponse[];
}