import { useEffect, useState } from "react";
import pollsApi from "../../api/pollsApi";
import type { PollResponse } from "../../types/poll";
import { getApiErrorMessage } from "../../utils/apiError";

interface GuestPollModalProps {
  poll: PollResponse;
  guestToken: string;
  isOpen: boolean;
  onClose: () => void;
  onVoted: (poll: PollResponse) => void;
}

const GuestPollModal = ({
  poll,
  guestToken,
  isOpen,
  onClose,
  onVoted,
}: GuestPollModalProps) => {
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setSelectedOptionId(null);
    setError(null);
    setIsSubmitting(false);
  }, [poll.id, isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleVote = async () => {
    if (selectedOptionId === null) {
      setError("Выберите один из вариантов");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const updatedPoll = await pollsApi.votePoll(poll.id, guestToken, {
        pollOptionId: selectedOptionId,
      });
      onVoted(updatedPoll);
      onClose();
    } catch (error: unknown) {
      setError(getApiErrorMessage(error, "Не удалось отправить голос"));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-950/50 px-4">
      <div className="w-full max-w-2xl rounded-3xl bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-blue-700">
              Активный опрос
            </p>
            <h2 className="mt-2 text-2xl font-bold text-slate-900">{poll.question}</h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
          >
            Закрыть
          </button>
        </div>

        <div className="mt-6 space-y-3">
          {poll.options.map((option) => {
            const checked = selectedOptionId === option.id;

            return (
              <label
                key={option.id}
                className={[
                  "flex cursor-pointer items-center gap-3 rounded-2xl border px-4 py-4 transition",
                  checked
                    ? "border-slate-900 bg-slate-50"
                    : "border-slate-200 hover:bg-slate-50",
                ].join(" ")}
              >
                <input
                  type="radio"
                  name="poll-option"
                  checked={checked}
                  onChange={() => setSelectedOptionId(option.id)}
                  className="h-4 w-4"
                />
                <span className="text-sm font-medium text-slate-700">{option.optionText}</span>
              </label>
            );
          })}
        </div>

        {error ? (
          <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            Позже
          </button>

          <button
            type="button"
            onClick={handleVote}
            disabled={isSubmitting}
            className="rounded-2xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {isSubmitting ? "Отправляем..." : "Проголосовать"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default GuestPollModal;