import type { RsvpCountersResponse } from "../../types/invitation";

interface GuestLiveCounterProps {
  counters: RsvpCountersResponse | null;
}

const GuestLiveCounter = ({ counters }: GuestLiveCounterProps) => {
  const items = [
    {
      label: "Идут",
      value: counters?.goingCount ?? 0,
      className: "border-emerald-200 bg-emerald-50 text-emerald-800",
    },
    {
      label: "Возможно",
      value: counters?.maybeCount ?? 0,
      className: "border-amber-200 bg-amber-50 text-amber-800",
    },
    {
      label: "Отказались",
      value: counters?.declinedCount ?? 0,
      className: "border-red-200 bg-red-50 text-red-800",
    },
    {
      label: "Ожидают",
      value: counters?.pendingCount ?? 0,
      className: "border-slate-200 bg-slate-50 text-slate-700",
    },
  ];

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-xl font-bold text-slate-900">Ответы гостей</h2>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
          Live
        </span>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {items.map((item) => (
          <div key={item.label} className={`rounded-2xl border p-4 ${item.className}`}>
            <p className="text-sm font-medium">{item.label}</p>
            <p className="mt-2 text-3xl font-bold">{item.value}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default GuestLiveCounter;