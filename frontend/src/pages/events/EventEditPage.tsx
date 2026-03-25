import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import eventsApi from "../../api/eventsApi";
import EventForm from "../../components/events/EventForm";
import type { EventRequest, EventResponse } from "../../types/event";
import { getApiErrorMessage } from "../../utils/apiError";

const EventEditPage = () => {
  const navigate = useNavigate();
  const { eventId } = useParams<{ eventId: string }>();

  const [eventData, setEventData] = useState<EventResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    const loadEvent = async () => {
      if (!eventId) {
        setLoadError("Идентификатор мероприятия не найден");
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setLoadError(null);

      try {
        const response = await eventsApi.getEventById(Number(eventId));
        setEventData(response);
      } catch (error: unknown) {
        setLoadError(getApiErrorMessage(error, "Не удалось загрузить данные мероприятия"));
      } finally {
        setIsLoading(false);
      }
    };

    void loadEvent();
  }, [eventId]);

  const handleSubmit = async ({
    event,
    poster,
  }: {
    event: EventRequest;
    poster: File | null;
  }) => {
    if (!eventId) {
      throw new Error("Идентификатор мероприятия не найден");
    }

    setIsSubmitting(true);

    try {
      await eventsApi.updateEvent(Number(eventId), event, poster);
      navigate(`/events/${eventId}/manage`);
    } finally {
      setIsSubmitting(false);
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
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Редактирование мероприятия
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          Обновите информацию о мероприятии, место проведения или постер.
        </p>
      </div>

      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <EventForm
          mode="edit"
          initialEvent={eventData}
          isSubmitting={isSubmitting}
          submitLabel="Сохранить изменения"
          onSubmit={handleSubmit}
        />
      </div>
    </div>
  );
};

export default EventEditPage;