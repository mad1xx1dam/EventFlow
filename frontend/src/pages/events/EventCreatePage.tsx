import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import eventsApi from "../../api/eventsApi";
import invitationsApi from "../../api/invitationsApi";
import EventForm from "../../components/events/EventForm";
import GuestEmailsTextarea from "../../components/events/GuestEmailsTextarea";
import type { EventRequest } from "../../types/event";

const toDefaultDateTimeLocal = (dateParam: string | null) => {
  if (!dateParam) {
    return "";
  }

  const date = new Date(`${dateParam}T12:00:00`);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const timezoneOffset = date.getTimezoneOffset();
  const localDate = new Date(date.getTime() - timezoneOffset * 60_000);

  return localDate.toISOString().slice(0, 16);
};

const parseGuestEmails = (value: string) => {
  return Array.from(
    new Set(
      value
        .split(/[\s,;]+/)
        .map((item) => item.trim())
        .filter(Boolean)
    )
  );
};

const EventCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [guestEmails, setGuestEmails] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const initialStartsAt = useMemo(
    () => toDefaultDateTimeLocal(searchParams.get("date")),
    [searchParams]
  );

  const handleSubmit = async ({
    event,
    poster,
  }: {
    event: EventRequest;
    poster: File | null;
  }) => {
    setIsSubmitting(true);

    try {
      const createdEvent = await eventsApi.createEvent(event, poster);

      const emails = parseGuestEmails(guestEmails);

      if (emails.length > 0) {
        await invitationsApi.createInvitations(createdEvent.id, {
          guestEmails: emails,
        });
      }

      navigate("/dashboard");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Создание мероприятия
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          Заполните основную информацию, выберите место на карте и при необходимости
          сразу добавьте гостей.
        </p>
      </div>

      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <EventForm
          mode="create"
          initialStartsAt={initialStartsAt}
          isSubmitting={isSubmitting}
          submitLabel="Создать мероприятие"
          onSubmit={handleSubmit}
        />
      </div>

      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900">Приглашения</h2>
        <p className="mt-2 text-sm text-slate-500">
          Укажите email гостей, которым нужно отправить приглашения после создания
          мероприятия.
        </p>

        <div className="mt-4">
          <GuestEmailsTextarea value={guestEmails} onChange={setGuestEmails} />
        </div>
      </div>
    </div>
  );
};

export default EventCreatePage;