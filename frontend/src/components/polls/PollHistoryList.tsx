import type { PollResponse } from "../../types/poll";
import PollResultsChart from "./PollResultsChart";

interface PollHistoryListProps {
  polls: PollResponse[];
  emptyText: string;
}

const PollHistoryList = ({ polls, emptyText }: PollHistoryListProps) => {
  if (polls.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500">
        {emptyText}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {polls.map((poll) => (
        <div key={poll.id} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <PollResultsChart poll={poll} />
        </div>
      ))}
    </div>
  );
};

export default PollHistoryList;