interface RsvpCountersProps {
  goingCount: number;
  maybeCount: number;
  declinedCount: number;
  pendingCount: number;
}

const RsvpCounters = ({
  goingCount,
  maybeCount,
  declinedCount,
  pendingCount,
}: RsvpCountersProps) => {
  const items = [
    {
      label: "Идут",
      value: goingCount,
      className: "border-emerald-200 bg-emerald-50 text-emerald-800",
    },
    {
      label: "Возможно",
      value: maybeCount,
      className: "border-amber-200 bg-amber-50 text-amber-800",
    },
    {
      label: "Отказались",
      value: declinedCount,
      className: "border-red-200 bg-red-50 text-red-800",
    },
    {
      label: "Ожидают",
      value: pendingCount,
      className: "border-slate-200 bg-slate-50 text-slate-700",
    },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {items.map((item) => (
        <div
          key={item.label}
          className={`rounded-2xl border p-4 shadow-sm ${item.className}`}
        >
          <p className="text-sm font-medium">{item.label}</p>
          <p className="mt-2 text-3xl font-bold">{item.value}</p>
        </div>
      ))}
    </div>
  );
};

export default RsvpCounters;