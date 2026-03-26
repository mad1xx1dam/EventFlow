import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import invitationsApi from "../../api/invitationsApi";
import pollsApi from "../../api/pollsApi";
import GuestEventCard from "../../components/guest/GuestEventCard";
import GuestLiveCounter from "../../components/guest/GuestLiveCounter";
import GuestPollModal from "../../components/guest/GuestPollModal";
import GuestRsvpPanel from "../../components/guest/GuestRsvpPanel";
import PollHistoryList from "../../components/polls/PollHistoryList";
import useStomp from "../../hooks/useStomp";
import type {
  GuestInvitationDetailsResponse,
  RsvpCountersResponse,
} from "../../types/invitation";
import type { PollResponse } from "../../types/poll";
import type {
  EventRsvpSnapshotMessage,
  PollLiveEventResponse,
} from "../../types/ws";
import { getApiErrorMessage } from "../../utils/apiError";

const upsertPoll = (polls: PollResponse[], incomingPoll: PollResponse) => {
  const nextPolls = polls.some((poll) => poll.id === incomingPoll.id)
    ? polls.map((poll) => (poll.id === incomingPoll.id ? incomingPoll : poll))
    : [incomingPoll, ...polls];

  return [...nextPolls].sort(
    (a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime()
  );
};

const GuestInvitationPage = () => {
  const { eventId, guestToken } = useParams<{
    eventId: string;
    guestToken: string;
  }>();

  const [invitation, setInvitation] = useState<GuestInvitationDetailsResponse | null>(null);
  const [counters, setCounters] = useState<RsvpCountersResponse | null>(null);
  const [polls, setPolls] = useState<PollResponse[]>([]);
  const [isPollOpen, setIsPollOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const numericEventId = Number(eventId);

  const { isConnected, subscribe } = useStomp({
    guestToken,
    enabled: Boolean(guestToken && eventId),
  });

  const activePoll = useMemo(
    () => polls.find((poll) => poll.status === "ACTIVE") ?? null,
    [polls]
  );

  const votableActivePoll = useMemo(
    () =>
      polls.find(
        (poll) => poll.status === "ACTIVE" && !poll.votedByCurrentGuest
      ) ?? null,
    [polls]
  );

  const closedPolls = useMemo(
    () => polls.filter((poll) => poll.status === "CLOSED"),
    [polls]
  );

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
        const [invitationResponse, pollsResponse] = await Promise.all([
          invitationsApi.getGuestInvitation(numericEventId, guestToken),
          pollsApi.getGuestPolls(numericEventId, guestToken),
        ]);

        setInvitation(invitationResponse);
        setCounters({
          eventId: invitationResponse.eventId,
          goingCount: Number(invitationResponse.goingCount),
          maybeCount: Number(invitationResponse.maybeCount),
          declinedCount: Number(invitationResponse.declinedCount),
          pendingCount: Number(invitationResponse.pendingCount),
        });
        setPolls(pollsResponse);
        setIsPollOpen(
          pollsResponse.some(
            (poll) => poll.status === "ACTIVE" && !poll.votedByCurrentGuest
          )
        );
      } catch (loadError: unknown) {
        setError(getApiErrorMessage(loadError, "Не удалось загрузить приглашение"));
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

    const unsubscribeRsvp = subscribe<EventRsvpSnapshotMessage>({
      destination: `/topic/events/${numericEventId}/rsvp`,
      onMessage: (payload) => {
        setCounters({
          eventId: payload.eventId,
          goingCount: Number(payload.goingCount),
          maybeCount: Number(payload.maybeCount),
          declinedCount: Number(payload.declinedCount),
          pendingCount: Number(payload.pendingCount),
        });
      },
    });

    const unsubscribePoll = subscribe<PollLiveEventResponse>({
      destination: `/topic/events/${numericEventId}/polls/active`,
      onMessage: (payload) => {
        setPolls((prev) => upsertPoll(prev, payload.poll));

        if (payload.type === "POLL_CLOSED") {
          setIsPollOpen(false);
          return;
        }

        setIsPollOpen((prev) => {
          if (prev) {
            return true;
          }

          return payload.poll.status === "ACTIVE" && !payload.poll.votedByCurrentGuest;
        });
      },
    });

    return () => {
      unsubscribeRsvp();
      unsubscribePoll();
    };
  }, [eventId, isConnected, numericEventId, subscribe]);

  useEffect(() => {
    if (!isConnected || !activePoll?.id) {
      return;
    }

    const unsubscribePollResults = subscribe<PollLiveEventResponse>({
      destination: `/topic/polls/${activePoll.id}/results`,
      onMessage: (payload) => {
        setPolls((prev) => {
          const currentActivePoll = prev.find((poll) => poll.id === payload.poll.id);

          if (currentActivePoll?.votedByCurrentGuest && !payload.poll.votedByCurrentGuest) {
            return upsertPoll(prev, {
              ...payload.poll,
              votedByCurrentGuest: true,
              selectedOptionId: currentActivePoll.selectedOptionId,
            });
          }

          return upsertPoll(prev, payload.poll);
        });
      },
    });

    return () => {
      unsubscribePollResults();
    };
  }, [activePoll?.id, isConnected, subscribe]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10">
        <div className="rounded-3xl border border-slate-200 bg-white px-6 py-10 text-center text-slate-500 shadow-sm">
          Загружаем приглашение...
        </div>
      </div>
    );
  }

  if (error || !invitation || !counters || !guestToken) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10">
        <div className="rounded-3xl border border-red-200 bg-red-50 px-6 py-10 text-center text-red-700 shadow-sm">
          {error ?? "Приглашение не найдено"}
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="mx-auto max-w-5xl space-y-6 px-4 py-8">
        <GuestEventCard invitation={invitation} />

        <GuestRsvpPanel
          eventId={numericEventId}
          guestToken={guestToken}
          currentStatus={invitation.rsvpStatus}
          onUpdated={(updatedInvitation) => {
            setInvitation(updatedInvitation);
            setCounters({
              eventId: updatedInvitation.eventId,
              goingCount: Number(updatedInvitation.goingCount),
              maybeCount: Number(updatedInvitation.maybeCount),
              declinedCount: Number(updatedInvitation.declinedCount),
              pendingCount: Number(updatedInvitation.pendingCount),
            });
          }}
        />

        <GuestLiveCounter counters={counters} />

        {activePoll && activePoll.votedByCurrentGuest ? (
          <section className="space-y-4">
            <h2 className="text-xl font-bold text-slate-900">Текущий опрос</h2>

            <div className="rounded-3xl border border-blue-200 bg-blue-50 p-6 shadow-sm">
              <div className="mb-4 flex flex-wrap items-center gap-2">
                <span className="inline-flex rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-800">
                  Активный
                </span>
                <span className="inline-flex rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800">
                  Вы уже проголосовали
                </span>
              </div>

              <h3 className="text-lg font-bold text-slate-900">{activePoll.question}</h3>

              {activePoll.selectedOptionId ? (
                <p className="mt-2 text-sm text-slate-600">
                  Ваш выбор:{" "}
                  <span className="font-semibold text-slate-900">
                    {activePoll.options.find(
                      (option) => option.id === activePoll.selectedOptionId
                    )?.optionText ?? "Вариант не найден"}
                  </span>
                </p>
              ) : null}

              <div className="mt-4">
                <PollHistoryList polls={[activePoll]} emptyText="" />
              </div>
            </div>
          </section>
        ) : null}

        <section className="space-y-4">
          <h2 className="text-xl font-bold text-slate-900">Прошлые опросы</h2>
          <PollHistoryList
            polls={closedPolls}
            emptyText="Прошлых опросов пока нет."
          />
        </section>
      </div>

      {votableActivePoll ? (
        <GuestPollModal
          poll={votableActivePoll}
          guestToken={guestToken}
          isOpen={isPollOpen}
          onClose={() => setIsPollOpen(false)}
          onVoted={(updatedPoll) => {
            setPolls((prev) => upsertPoll(prev, updatedPoll));
            setIsPollOpen(false);
          }}
        />
      ) : null}
    </>
  );
};

export default GuestInvitationPage;