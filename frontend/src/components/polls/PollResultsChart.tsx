import type { PollResponse } from "../../types/poll";

interface PollResultsChartProps {
  poll: PollResponse;
}

const formatDateTime = (value: string | null) => {
  if (!value) {
    return null;
  }

  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
};

const PollResultsChart = ({ poll }: PollResultsChartProps) => {
  const totalVotes = poll.options.reduce((sum, option) => sum + option.votesCount, 0);
  const closedAtLabel = formatDateTime(poll.closedAt);

  return (
    <div className="space-y-4">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <span
            className={[
              "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold",
              poll.status === "ACTIVE"
                ? "bg-blue-100 text-blue-800"
                : "bg-slate-100 text-slate-700",
            ].join(" ")}
          >
            {poll.status === "ACTIVE" ? "Активный" : "Закрыт"}
          </span>
        </div>

        <h3 className="mt-3 text-lg font-bold text-slate-900">{poll.question}</h3>

        <p className="mt-1 text-sm text-slate-500">
          Всего голосов: {totalVotes}
          {closedAtLabel ? ` · Завершён ${closedAtLabel}` : ""}
        </p>
      </div>

      <div className="space-y-4">
        {poll.options.map((option) => {
          const percentage = totalVotes === 0 ? 0 : Math.round((option.votesCount / totalVotes) * 100);

          return (
            <div key={option.id} className="space-y-2">
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="font-medium text-slate-700">{option.optionText}</span>
                <span className="text-slate-500">
                  {option.votesCount} голос(ов) · {percentage}%
                </span>
              </div>

              <div className="h-3 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-slate-900 transition-all"
                  style={{ width: `${percentage}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default PollResultsChart;