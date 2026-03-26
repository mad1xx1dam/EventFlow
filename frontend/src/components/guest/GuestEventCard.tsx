import type { GuestInvitationDetailsResponse, RsvpStatus } from "../../types/invitation";
import EventMapPreview from "../map/EventMapPreview";

interface GuestEventCardProps {
  invitation: GuestInvitationDetailsResponse;
}

const statusLabelMap: Record<RsvpStatus, string> = {
  PENDING: "Ожидает ответа",
  GOING: "Вы идёте",
  MAYBE: "Вы возможно придёте",
  DECLINED: "Вы отказались",
};

const statusClassMap: Record<RsvpStatus, string> = {
  PENDING: "bg-slate-100 text-slate-700",
  GOING: "bg-emerald-100 text-emerald-800",
  MAYBE: "bg-amber-100 text-amber-800",
  DECLINED: "bg-red-100 text-red-800",
};

const formatDateTime = (value: string) => {
  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "full",
    timeStyle: "short",
  }).format(new Date(value));
};

const GuestEventCard = ({ invitation }: GuestEventCardProps) => {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">
            {invitation.title}
          </h1>
          <p className="mt-2 text-sm text-slate-500">
            {formatDateTime(invitation.startsAt)}
          </p>
        </div>

        <span
          className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${statusClassMap[invitation.rsvpStatus]}`}
        >
          {statusLabelMap[invitation.rsvpStatus]}
        </span>
      </div>

      {invitation.description ? (
        <p className="mt-4 text-sm leading-6 text-slate-600">
          {invitation.description}
        </p>
      ) : null}

      <div className="mt-5 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
        <span className="font-medium text-slate-700">Адрес:</span> {invitation.address}
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <h2 className="mb-4 text-base font-semibold text-slate-900">Постер</h2>

          {invitation.posterUrl ? (
            <div className="flex min-h-[320px] items-center justify-center rounded-2xl bg-white p-4">
              <img
                src={invitation.posterUrl}
                alt="Постер мероприятия"
                className="max-h-[420px] w-auto rounded-xl border border-slate-200 object-contain"
              />
            </div>
          ) : (
            <div className="flex min-h-[320px] items-center justify-center rounded-2xl bg-white px-4 py-10 text-center text-sm text-slate-500">
              Постер не загружен
            </div>
          )}
        </div>

        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <h2 className="mb-4 text-base font-semibold text-slate-900">Место на карте</h2>

          <div className="overflow-hidden rounded-2xl">
            <EventMapPreview lat={invitation.lat} lon={invitation.lon} />
          </div>
        </div>
      </div>

      <div className="mt-4 text-sm text-slate-500">
        Приглашение отправлено на{" "}
        <span className="font-medium text-slate-700">{invitation.guestEmail}</span>
      </div>
    </div>
  );
};

export default GuestEventCard;