import type { InvitationResponse } from "./invitation";
import type { PollResponse } from "./poll";

export interface PollLiveEventResponse {
  type: "POLL_STARTED" | "POLL_UPDATED" | "POLL_CLOSED";
  poll: PollResponse;
}

export interface EventRsvpSnapshotMessage {
  eventId: number;
  guests: InvitationResponse[];
  goingCount: number;
  maybeCount: number;
  declinedCount: number;
  pendingCount: number;
}