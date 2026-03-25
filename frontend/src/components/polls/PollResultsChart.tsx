import type { PollResponse } from "../../types/poll";

interface PollResultsChartProps {
  poll: PollResponse;
}

const PollResultsChart = ({ poll }: PollResultsChartProps) => {
  const totalVotes = poll.options.reduce((sum, option) => sum + option.votesCount, 0);

  return (
    <div className="space-y-4">
      <div>
        <h3 className="text-lg font-bold text-slate-900">Результаты опроса</h3>
        <p className="mt-1 text-sm text-slate-500">
          Всего голосов: {totalVotes}
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