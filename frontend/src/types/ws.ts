import type { PollResponse } from "./poll";
import type { RsvpCountersResponse } from "./invitation";

export type PollLiveEventType = "POLL_STARTED" | "POLL_UPDATED" | "POLL_CLOSED";

export interface PollLiveEventResponse {
  type: PollLiveEventType;
  poll: PollResponse;
}

export interface EventRsvpCountersMessage extends RsvpCountersResponse {}

export interface EventPollTopicMessage extends PollLiveEventResponse {}

export interface PollResultsTopicMessage extends PollLiveEventResponse }

export interface WsJwtConnectHeaders {
  Authorization: string;
}

export interface WsGuestConnectHeaders {
  "Guest-Token": string;
}

export interface WsSubscriptionConfig {
  destination: string;
  headers?: Record<string, string>;
}