import { useEffect, useMemo, useState } from "react";
import { useLocation, useParams } from "react-router-dom";
import eventsApi from "../../api/eventsApi";
import invitationsApi from "../../api/invitationsApi";
import pollsApi from "../../api/pollsApi";
import EventDetailsCard from "../../components/events/EventDetailsCard";
import GuestEmailsTextarea from "../../components/events/GuestEmailsTextarea";
import RsvpCounters from "../../components/guests/RsvpCounters";
import GuestListTable from "../../components/guests/GuestListTable";
import EventMapPreview from "../../components/map/EventMapPreview";
import ActivePollBanner from "../../components/polls/ActivePollBanner";
import PollCreateForm from "../../components/polls/PollCreateForm";
import PollResultsChart from "../../components/polls/PollResultsChart";
import Button from "../../components/common/Button";
import type { EventResponse } from "../../types/event";
import type { InvitationResponse } from "../../types/invitation";
import type { PollResponse } from "../../types/poll";
import type { EventRsvpSnapshotMessage, PollLiveEventResponse } from "../../types/ws";
import useStomp from "../../hooks/useStomp";
import { getApiErrorMessage, getApiErrorStatus } from "../../utils/apiError";

interface EventManageLocationState {
  pageMessage?: string;
  pageMessageType?: "success" | "warning" | "error";
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/i;

const parseGuestEmails = (value: string) => {
  const rawTokens = value
    .split(/[\s,;]+/)
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);

  const validEmails: string[] = [];
  const invalidEmails: string[] = [];
  const duplicateEmails: string[] = [];

  const seen = new Set<string>();
  const duplicateSeen = new Set<string>();

  for (const token of rawTokens) {
    if (!EMAIL_PATTERN.test(token)) {
      invalidEmails.push(token);
      continue;
    }

    if (seen.has(token)) {
      if (!duplicateSeen.has(token)) {
        duplicateEmails.push(token);
        duplicateSeen.add(token);
      }
      continue;
    }

    seen.add(token);
    validEmails.push(token);
  }

  return {
    validEmails,
    invalidEmails,
    duplicateEmails,
  };
};

const getCountersFromInvitations = (invitations: InvitationResponse[]) => {
  return invitations.reduce(
    (acc, invitation) => {
      if (invitation.rsvpStatus === "GOING") {
        acc.goingCount += 1;
      }

      if (invitation.rsvpStatus === "MAYBE") {
        acc.maybeCount += 1;
      }

      if (invitation.rsvpStatus === "DECLINED") {
        acc.declinedCount += 1;
      }

      if (invitation.rsvpStatus === "PENDING") {
        acc.pendingCount += 1;
      }

      return acc;
    },
    {
      goingCount: 0,
      maybeCount: 0,
      declinedCount: 0,
      pendingCount: 0,
    }
  );
};

const EventManagePage = () => {
  const { eventId } = useParams<{ eventId: string }>();
  const location = useLocation();
  const locationState = location.state as EventManageLocationState | null;

  const numericEventId = Number(eventId);
  const accessToken = localStorage.getItem("accessToken");

  const [eventData, setEventData] = useState<EventResponse | null>(null);
  const [guests, setGuests] = useState<InvitationResponse[]>([]);
  const [activePoll, setActivePoll] = useState<PollResponse | null>(null);
  const [lastPoll, setLastPoll] = useState<PollResponse | null>(null);
  const [guestEmails, setGuestEmails] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [guestsMessage, setGuestsMessage] = useState<string | null>(locationState?.pageMessage ?? null);
  const [guestsMessageType, setGuestsMessageType] = useState<
    "success" | "warning" | "error" | null
  >(locationState?.pageMessageType ?? null);
  const [isSubmittingGuests, setIsSubmittingGuests] = useState(false);

  const { subscribe } = useStomp({
    token: accessToken,
    enabled: Boolean(accessToken),
  });

  const counters = useMemo(() => getCountersFromInvitations(guests), [guests]);

  useEffect(() => {
    const loadPage = async () => {
      if (!eventId || Number.isNaN(numericEventId)) {
        setLoadError("Некорректный идентификатор мероприятия");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setLoadError(null);

      try {
        const [eventResponse, guestsResponse] = await Promise.all([
          eventsApi.getEventById(numericEventId),
          eventsApi.getEventGuests(numericEventId),
        ]);

        setEventData(eventResponse);
        setGuests(guestsResponse);

        try {
          const pollResponse = await pollsApi.getActivePoll(numericEventId);
          setActivePoll(pollResponse);
          setLastPoll(pollResponse);
        } catch {
          setActivePoll(null);
        }
      } catch (error: unknown) {
        const status = getApiErrorStatus(error);
        setLoadError(
          status === 404
            ? "Мероприятие не найдено"
            : getApiErrorMessage(error, "Не удалось загрузить страницу управления мероприятием")
        );
      } finally {
        setIsLoading(false);
      }
    };

    void loadPage();
  }, [eventId, numericEventId]);

  useEffect(() => {
    if (!eventId || Number.isNaN(numericEventId) || !accessToken) {
      return;
    }

    const unsubscribeRsvp = subscribe<EventRsvpSnapshotMessage>({
      destination: `/topic/events/${numericEventId}/rsvp`,
      onMessage: (payload) => {
        setGuests(payload.guests);
      },
    });

    const unsubscribeEventPoll = subscribe<PollLiveEventResponse>({
      destination: `/topic/events/${numericEventId}/polls/active`,
      onMessage: (payload) => {
        if (payload.type === "POLL_CLOSED") {
          setActivePoll(null);
          setLastPoll(payload.poll);
          return;
        }

        setActivePoll(payload.poll);
        setLastPoll(payload.poll);
      },
    });

    return () => {
      unsubscribeRsvp();
      unsubscribeEventPoll();
    };
  }, [eventId, numericEventId, subscribe, accessToken]);

  useEffect(() => {
    if (!activePoll?.id || !accessToken) {
      return;
    }

    const unsubscribePollResults = subscribe<PollLiveEventResponse>({
      destination: `/topic/polls/${activePoll.id}/results`,
      onMessage: (payload) => {
        setActivePoll(payload.poll);
        setLastPoll(payload.poll);
      },
    });

    return () => {
      unsubscribePollResults();
    };
  }, [activePoll?.id, subscribe, accessToken]);

  const handleAddGuests = async () => {
    const { validEmails, invalidEmails, duplicateEmails } = parseGuestEmails(guestEmails);

    if (validEmails.length === 0) {
      setGuestsMessage(
        invalidEmails.length > 0
          ? `Добавьте хотя бы один корректный email. Некорректные: ${invalidEmails.join(", ")}`
          : "Добавьте хотя бы один email гостя"
      );
      setGuestsMessageType("warning");
      return;
    }

    setIsSubmittingGuests(true);

    try {
      const createdInvitations = await invitationsApi.createInvitations(numericEventId, {
        guestEmails: validEmails,
      });

      setGuests((prev) => {
        const merged = [...prev];
        createdInvitations.forEach((invitation) => {
          const index = merged.findIndex((item) => item.id === invitation.id);
          if (index >= 0) {
            merged[index] = invitation;
          } else {
            merged.push(invitation);
          }
        });

        return merged;
      });

      const messageParts: string[] = [];

      if (createdInvitations.length > 0) {
        messageParts.push(`Приглашения отправлены: ${createdInvitations.length}`);
      }

      if (duplicateEmails.length > 0) {
        messageParts.push(`Дубликаты в форме: ${duplicateEmails.join(", ")}`);
      }

      if (invalidEmails.length > 0) {
        messageParts.push(`Некорректные email: ${invalidEmails.join(", ")}`);
      }

      setGuestsMessage(messageParts.join(". "));
      setGuestsMessageType("success");
      setGuestEmails("");
    } catch (error: unknown) {
      setGuestsMessage(getApiErrorMessage(error, "Не удалось отправить приглашения"));
      setGuestsMessageType("error");
    } finally {
      setIsSubmittingGuests(false);
    }
  };

  if (isLoading) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
        Загрузка мероприятия...
      </div>
    );
  }

  if (loadError || !eventData) {
    return (
      <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
        {loadError ?? "Мероприятие не найдено"}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="space-y-4">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Управление мероприятием</h1>

        <EventDetailsCard event={eventData} />

        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <EventMapPreview lat={eventData.lat} lon={eventData.lon} />
        </div>

        {guestsMessage ? (
          <div
            className={[
              "rounded-2xl px-4 py-3 text-sm border",
              guestsMessageType === "success"
                ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                : guestsMessageType === "warning"
                  ? "border-amber-200 bg-amber-50 text-amber-800"
                  : "border-red-200 bg-red-50 text-red-700",
            ].join(" ")}
          >
            {guestsMessage}
          </div>
        ) : null}

        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">Пригласить ещё гостей</h3>
              <p className="mt-1 text-sm text-slate-500">
                Добавьте новые email. Уже существующие приглашения не будут продублированы.
              </p>
            </div>

            <GuestEmailsTextarea value={guestEmails} onChange={setGuestEmails} />

            <div className="sm:w-64">
              <Button type="button" isLoading={isSubmittingGuests} onClick={handleAddGuests}>
                Добавить гостей
              </Button>
            </div>
          </div>
        </div>

        <RsvpCounters
          goingCount={counters.goingCount}
          maybeCount={counters.maybeCount}
          declinedCount={counters.declinedCount}
          pendingCount={counters.pendingCount}
        />

        <GuestListTable invitations={guests} />
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-bold text-slate-900">Опросы</h2>

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

        {lastPoll ? (
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <PollResultsChart poll={lastPoll} />
          </div>
        ) : null}
      </section>
    </div>
  );
};

export default EventManagePage;