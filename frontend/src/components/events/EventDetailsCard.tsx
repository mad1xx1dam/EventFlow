import type { EventResponse } from "../../types/event";

interface EventDetailsCardProps {
  event: EventResponse;
}

const statusLabelMap: Record<EventResponse["status"], string> = {
  ACTIVE: "Активно",
  CANCELLED: "Отменено",
};

const statusClassMap: Record<EventResponse["status"], string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800",
  CANCELLED: "bg-red-100 text-red-800",
};

const formatDateTime = (value: string) => {
  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "full",
    timeStyle: "short",
  }).format(new Date(value));
};

const EventDetailsCard = ({ event }: EventDetailsCardProps) => {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">{event.title}</h2>
          <p className="mt-2 text-sm text-slate-500">
            {formatDateTime(event.startsAt)}
          </p>
        </div>

        <span
          className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${statusClassMap[event.status]}`}
        >
          {statusLabelMap[event.status]}
        </span>
      </div>

      {event.description ? (
        <p className="mt-4 text-sm leading-6 text-slate-600">
          {event.description}
        </p>
      ) : null}

      <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
        <span className="font-medium text-slate-700">Адрес:</span> {event.address}
      </div>
    </div>
  );
};

export default EventDetailsCard;