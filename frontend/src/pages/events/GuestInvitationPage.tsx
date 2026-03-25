import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import invitationsApi from "../../api/invitationsApi";
import useStomp from "../../hooks/useStomp";
import GuestEventCard from "../../components/guest/GuestEventCard";
import GuestRsvpPanel from "../../components/guest/GuestRsvpPanel";
import GuestLiveCounter from "../../components/guest/GuestLiveCounter";
import GuestPollModal from "../../components/guest/GuestPollModal";
import type {
  GuestInvitationDetailsResponse,
  RsvpCountersResponse,
} from "../../types/invitation";
import type { PollLiveEventResponse } from "../../types/ws";
import type { PollResponse } from "../../types/poll";
import { getApiErrorMessage } from "../../utils/apiError";

const GuestInvitationPage = () => {
  const { eventId, guestToken } = useParams<{
    eventId: string;
    guestToken: string;
  }>();

  const [invitation, setInvitation] = useState<GuestInvitationDetailsResponse | null>(null);
  const [counters, setCounters] = useState<RsvpCountersResponse | null>(null);
  const [activePoll, setActivePoll] = useState<PollResponse | null>(null);
  const [isPollOpen, setIsPollOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const numericEventId = Number(eventId);

  const { isConnected, subscribe } = useStomp({
    guestToken,
    enabled: Boolean(guestToken && eventId),
  });

  useEffect(() => {
    const loadInvitation = async () => {
      if (!guestToken || !eventId || Number.isNaN(numericEventId)) {
        setError("Некорректная ссылка приглашения");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const response = await invitationsApi.getGuestInvitation(numericEventId, guestToken);
        setInvitation(response);
      } catch (error: unknown) {
        setError(getApiErrorMessage(error, "Не удалось загрузить приглашение"));
      } finally {
        setIsLoading(false);
      }
    };

    void loadInvitation();
  }, [eventId, guestToken, numericEventId]);

  useEffect(() => {
    if (!isConnected || !eventId || Number.isNaN(numericEventId)) {
      return;
    }

    const unsubscribeRsvp = subscribe<RsvpCountersResponse>({
      destination: `/topic/events/${numericEventId}/rsvp-counters`,
      onMessage: (payload) => {
        setCounters(payload);
      },
    });

    const unsubscribePoll = subscribe<PollLiveEventResponse>({
      destination: `/topic/events/${numericEventId}/polls/active`,
      onMessage: (payload) => {
        if (payload.type === "POLL_STARTED" || payload.type === "POLL_UPDATED") {
          setActivePoll(payload.poll);
          setIsPollOpen(true);
        }

        if (payload.type === "POLL_CLOSED") {
          setActivePoll(null);
          setIsPollOpen(false);
        }
      },
    });

    return () => {
      unsubscribeRsvp();
      unsubscribePoll();
    };
  }, [eventId, isConnected, numericEventId, subscribe]);

  if (isLoading) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
        Загрузка приглашения...
      </div>
    );
  }

  if (error || !invitation || !guestToken) {
    return (
      <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
        {error ?? "Приглашение не найдено"}
      </div>
    );
  }

  return (
    <>
      <div className="space-y-6">
        {!isConnected ? (
          <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Live-обновления пока недоступны. Пытаемся подключиться...
          </div>
        ) : null}

        <GuestEventCard invitation={invitation} />

        <GuestRsvpPanel
          eventId={invitation.eventId}
          guestToken={guestToken}
          currentStatus={invitation.rsvpStatus}
          onUpdated={(updatedInvitation) => {
            setInvitation(updatedInvitation);
          }}
        />

        <GuestLiveCounter counters={counters} />
      </div>

      {activePoll ? (
        <GuestPollModal
          poll={activePoll}
          guestToken={guestToken}
          isOpen={isPollOpen}
          onClose={() => setIsPollOpen(false)}
          onVoted={(updatedPoll) => {
            setActivePoll(updatedPoll);
          }}
        />
      ) : null}
    </>
  );
};

export default GuestInvitationPage;