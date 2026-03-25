import { useState } from "react";
import pollsApi from "../../api/pollsApi";
import Button from "../common/Button";
import type { PollResponse } from "../../types/poll";
import { getApiErrorMessage } from "../../utils/apiError";

interface ActivePollBannerProps {
  poll: PollResponse;
  onClosed: (poll: PollResponse) => void;
}

const ActivePollBanner = ({ poll, onClosed }: ActivePollBannerProps) => {
  const [error, setError] = useState<string | null>(null);
  const [isClosing, setIsClosing] = useState(false);

  const handleClose = async () => {
    setIsClosing(true);
    setError(null);

    try {
      const closedPoll = await pollsApi.closePoll(poll.id);
      onClosed(closedPoll);
    } catch (error: unknown) {
      setError(getApiErrorMessage(error, "Не удалось завершить опрос"));
    } finally {
      setIsClosing(false);
    }
  };

  return (
    <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-blue-700">
            Активный опрос
          </p>
          <h3 className="mt-2 text-xl font-bold text-slate-900">{poll.question}</h3>
          <p className="mt-2 text-sm text-slate-600">
            Вариантов ответа: {poll.options.length}
          </p>
        </div>

        <div className="w-full lg:w-56">
          <Button type="button" isLoading={isClosing} onClick={handleClose}>
            Завершить
          </Button>
        </div>
      </div>

      {error ? <p className="mt-4 text-sm text-red-700">{error}</p> : null}
    </div>
  );
};

export default ActivePollBanner;