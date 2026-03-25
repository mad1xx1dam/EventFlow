import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import eventsApi from "../../api/eventsApi";
import invitationsApi from "../../api/invitationsApi";
import pollsApi from "../../api/pollsApi";
import EventDetailsCard from "../../components/events/EventDetailsCard";
import RsvpCounters from "../../components/guests/RsvpCounters";
import GuestListTable from "../../components/guests/GuestListTable";
import EventMapPreview from "../../components/map/EventMapPreview";
import ActivePollBanner from "../../components/polls/ActivePollBanner";
import PollCreateForm from "../../components/polls/PollCreateForm";
import PollResultsChart from "../../components/polls/PollResultsChart";
import Button from "../../components/common/Button";
import type { EventResponse } from "../../types/event";
import type {
  InvitationResponse,
  RsvpCountersResponse,
  RsvpStatus,
} from "../../types/invitation";
import type { PollResponse } from "../../types/poll";
import type { PollLiveEventResponse } from "../../types/ws";
import useStomp from "../../hooks/useStomp";
import { getApiErrorMessage, getApiErrorStatus } from "../../utils/apiError";

const EventManagePage = () => {
  const { eventId } = useParams<{ eventId: string }>();

  const [eventData, setEventData] = useState<EventResponse | null>(null);
  const [guests, setGuests] = useState<InvitationResponse[]>([]);
  const [activePoll, setActivePoll] = useState<PollResponse | null>(null);
  const [lastPoll, setLastPoll] = useState<PollResponse | null>(null);
  const [liveCounters, setLiveCounters] = useState<RsvpCountersResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isResending, setIsResending] = useState(false);
  const [guestsMessage, setGuestsMessage] = useState<string | null>(null);
  const [guestsMessageType, setGuestsMessageType] = useState<"success" | "error">("success");
  const [error, setError] = useState<string | null>(null);

  const numericEventId = useMemo(() => Number(eventId), [eventId]);
  const token = localStorage.getItem("accessToken");

  const { isConnected, subscribe } = useStomp({
    token,
    enabled: Boolean(token && eventId),
  });

  useEffect(() => {
    const loadData = async () => {
      if (!eventId || Number.isNaN(numericEventId)) {
        setError("Идентификатор мероприятия не найден");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const [eventResponse, guestsResponse, pollResponse] = await Promise.all([
          eventsApi.getEventById(numericEventId),
          eventsApi.getEventGuests(numericEventId),
          pollsApi.getActivePoll(numericEventId).catch((error: unknown) => {
            const status = getApiErrorStatus(error);
            const message = getApiErrorMessage(error, "").toLowerCase();

            if (status === 404 || message.includes("не найден")) {
              return null;
            }

            throw error;
          }),
        ]);

        setEventData(eventResponse);
        setGuests(guestsResponse);
        setActivePoll(pollResponse);
        setLastPoll(pollResponse);
      } catch (error: unknown) {
        setError(getApiErrorMessage(error, "Не удалось загрузить данные мероприятия"));
      } finally {
        setIsLoading(false);
      }
    };

    void loadData();
  }, [eventId, numericEventId]);

  useEffect(() => {
    if (!isConnected || !eventId || Number.isNaN(numericEventId)) {
      return;
    }

    const unsubscribeRsvp = subscribe<RsvpCountersResponse>({
      destination: `/topic/events/${numericEventId}/rsvp-counters`,
      onMessage: (payload) => {
        setLiveCounters(payload);
      },
    });

    const unsubscribeActivePoll = subscribe<PollLiveEventResponse>({
      destination: `/topic/events/${numericEventId}/polls/active`,
      onMessage: (payload) => {
        if (payload.type === "POLL_STARTED" || payload.type === "POLL_UPDATED") {
          setActivePoll(payload.poll);
          setLastPoll(payload.poll);
        }

        if (payload.type === "POLL_CLOSED") {
          setActivePoll(null);
          setLastPoll(payload.poll);
        }
      },
    });

    return () => {
      unsubscribeRsvp();
      unsubscribeActivePoll();
    };
  }, [eventId, isConnected, numericEventId, subscribe]);

  useEffect(() => {
    if (!isConnected || !activePoll?.id) {
      return;
    }

    const unsubscribePollResults = subscribe<PollLiveEventResponse>({
      destination: `/topic/polls/${activePoll.id}/results`,
      onMessage: (payload) => {
        setLastPoll(payload.poll);

        if (payload.poll.status === "ACTIVE") {
          setActivePoll(payload.poll);
        } else {
          setActivePoll(null);
        }
      },
    });

    return () => {
      unsubscribePollResults();
    };
  }, [activePoll?.id, isConnected, subscribe]);

  const buildCounterInvitations = (): InvitationResponse[] => {
    if (!liveCounters) {
      return guests;
    }

    const currentGoing = guests.filter((item) => item.rsvpStatus === "GOING").length;
    const currentMaybe = guests.filter((item) => item.rsvpStatus === "MAYBE").length;
    const currentDeclined = guests.filter((item) => item.rsvpStatus === "DECLINED").length;
    const currentPending = guests.filter((item) => item.rsvpStatus === "PENDING").length;

    if (
      currentGoing === liveCounters.goingCount &&
      currentMaybe === liveCounters.maybeCount &&
      currentDeclined === liveCounters.declinedCount &&
      currentPending === liveCounters.pendingCount
    ) {
      return guests;
    }

    const normalizedStatuses: RsvpStatus[] = [
      ...Array.from({ length: liveCounters.goingCount }, () => "GOING" as const),
      ...Array.from({ length: liveCounters.maybeCount }, () => "MAYBE" as const),
      ...Array.from({ length: liveCounters.declinedCount }, () => "DECLINED" as const),
      ...Array.from({ length: liveCounters.pendingCount }, () => "PENDING" as const),
    ];

    return guests.map((guest, index) => ({
      ...guest,
      rsvpStatus: normalizedStatuses[index] ?? guest.rsvpStatus,
    }));
  };

  const handleResendInvitations = async () => {
    if (!eventId || Number.isNaN(numericEventId)) {
      return;
    }

    setIsResending(true);
    setGuestsMessage(null);

    try {
      const response = await invitationsApi.resendInvitations(numericEventId);
      setGuests(response);
      setGuestsMessage("Приглашения были повторно отправлены");
      setGuestsMessageType("success");
    } catch (error: unknown) {
      setGuestsMessage(getApiErrorMessage(error, "Не удалось повторно отправить приглашения"));
      setGuestsMessageType("error");
    } finally {
      setIsResending(false);
    }
  };

  if (isLoading) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
        Загрузка данных мероприятия...
      </div>
    );
  }

  if (error || !eventData) {
    return (
      <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
        {error ?? "Мероприятие не найдено"}
      </div>
    );
  }

  const counterInvitations = buildCounterInvitations();

  return (
    <div className="space-y-8">
      {!isConnected ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Live-обновления временно недоступны. Пытаемся подключиться...
        </div>
      ) : null}

      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Управление мероприятием
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          Просматривайте детали события, контролируйте ответы гостей и запускайте опросы.
        </p>
      </div>

      <section className="space-y-4">
        <h2 className="text-xl font-bold text-slate-900">Детали</h2>

        <div className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
          <EventDetailsCard event={eventData} />
          <EventMapPreview lat={eventData.lat} lon={eventData.lon} />
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-xl font-bold text-slate-900">Гости</h2>

          <div className="w-full sm:w-64">
            <Button type="button" isLoading={isResending} onClick={handleResendInvitations}>
              Повторно отправить приглашения
            </Button>
          </div>
        </div>

        {guestsMessage ? (
          <div
            className={[
              "rounded-2xl px-4 py-3 text-sm",
              guestsMessageType === "success"
                ? "border border-emerald-200 bg-emerald-50 text-emerald-800"
                : "border border-red-200 bg-red-50 text-red-700",
            ].join(" ")}
          >
            {guestsMessage}
          </div>
        ) : null}

        <RsvpCounters invitations={counterInvitations} />
        <GuestListTable invitations={guests} />
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-bold text-slate-900">Опросы</h2>

        <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            {activePoll ? (
              <ActivePollBanner
                poll={activePoll}
                onClosed={(closedPoll) => {
                  setActivePoll(null);
                  setLastPoll(closedPoll);
                }}
              />
            ) : (
              <PollCreateForm
                eventId={numericEventId}
                onCreated={(poll) => {
                  setActivePoll(poll);
                  setLastPoll(poll);
                }}
              />
            )}
          </div>

          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            {lastPoll ? (
              <PollResultsChart poll={lastPoll} />
            ) : (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500">
                Опрос пока не создан.
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
};

export default EventManagePage;