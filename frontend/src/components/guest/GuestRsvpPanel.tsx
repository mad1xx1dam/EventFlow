import { useState } from "react";
import invitationsApi from "../../api/invitationsApi";
import type {
  GuestInvitationDetailsResponse,
  RsvpStatus,
} from "../../types/invitation";
import { getApiErrorMessage } from "../../utils/apiError";

interface GuestRsvpPanelProps {
  eventId: number;
  guestToken: string;
  currentStatus: RsvpStatus;
  onUpdated: (invitation: GuestInvitationDetailsResponse) => void;
}

const buttons: Array<{
  status: Exclude<RsvpStatus, "PENDING">;
  label: string;
  className: string;
}> = [
  {
    status: "GOING",
    label: "Иду",
    className: "bg-emerald-600 hover:bg-emerald-700",
  },
  {
    status: "MAYBE",
    label: "Возможно",
    className: "bg-amber-500 hover:bg-amber-600",
  },
  {
    status: "DECLINED",
    label: "Отказался",
    className: "bg-red-600 hover:bg-red-700",
  },
];

const GuestRsvpPanel = ({
  eventId,
  guestToken,
  currentStatus,
  onUpdated,
}: GuestRsvpPanelProps) => {
  const [isSubmitting, setIsSubmitting] = useState<RsvpStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleUpdate = async (status: Exclude<RsvpStatus, "PENDING">) => {
    setIsSubmitting(status);
    setError(null);

    try {
      const updated = await invitationsApi.updateRsvp(eventId, guestToken, {
        rsvpStatus: status,
      });
      onUpdated(updated);
    } catch (error: unknown) {
      setError(getApiErrorMessage(error, "Не удалось обновить RSVP-статус"));
    } finally {
      setIsSubmitting(null);
    }
  };

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-bold text-slate-900">Ваш ответ</h2>
      <p className="mt-2 text-sm text-slate-500">
        Выберите статус участия. Его можно изменить позже.
      </p>

      <div className="mt-5 grid gap-3 sm:grid-cols-3">
        {buttons.map((button) => {
          const isActive = currentStatus === button.status;
          const isLoading = isSubmitting === button.status;

          return (
            <button
              key={button.status}
              type="button"
              onClick={() => handleUpdate(button.status)}
              disabled={isSubmitting !== null}
              className={[
                "rounded-2xl px-4 py-4 text-sm font-semibold text-white transition disabled:cursor-not-allowed disabled:opacity-70",
                button.className,
                isActive ? "ring-4 ring-offset-2 ring-slate-200" : "",
              ].join(" ")}
            >
              {isLoading ? "Сохраняем..." : button.label}
            </button>
          );
        })}
      </div>

      {error ? (
        <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}
    </div>
  );
};

export default GuestRsvpPanel;