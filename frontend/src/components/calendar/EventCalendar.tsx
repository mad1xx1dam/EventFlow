import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import dashboardApi from "../../api/dashboardApi";
import type { CalendarEventItemResponse } from "../../types/calendar";

interface EventCalendarProps {
  year: number;
  month: number;
  onPrevMonth: () => void;
  onNextMonth: () => void;
}

type CalendarDay = {
  date: Date;
  isCurrentMonth: boolean;
};

type DayEvent = CalendarEventItemResponse & {
  source: "creator" | "guest";
};

const weekDays = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];

const formatDateKey = (date: Date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");

  return `${y}-${m}-${d}`;
};

const formatMonthTitle = (year: number, month: number) => {
  return new Intl.DateTimeFormat("ru-RU", {
    month: "long",
    year: "numeric",
  }).format(new Date(year, month - 1, 1));
};

const formatEventTime = (startsAt: string) => {
  return new Intl.DateTimeFormat("ru-RU", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(startsAt));
};

const getCalendarDays = (year: number, month: number): CalendarDay[] => {
  const firstDayOfMonth = new Date(year, month - 1, 1);
  const lastDayOfMonth = new Date(year, month, 0);

  const startWeekday = (firstDayOfMonth.getDay() + 6) % 7;
  const daysInMonth = lastDayOfMonth.getDate();

  const prevMonthLastDay = new Date(year, month - 1, 0).getDate();

  const days: CalendarDay[] = [];

  for (let i = startWeekday - 1; i >= 0; i -= 1) {
    days.push({
      date: new Date(year, month - 2, prevMonthLastDay - i),
      isCurrentMonth: false,
    });
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    days.push({
      date: new Date(year, month - 1, day),
      isCurrentMonth: true,
    });
  }

  const remainingCells = (7 - (days.length % 7)) % 7;

  for (let day = 1; day <= remainingCells; day += 1) {
    days.push({
      date: new Date(year, month, day),
      isCurrentMonth: false,
    });
  }

  return days;
};

const EventCalendar = ({ year, month, onPrevMonth, onNextMonth }: EventCalendarProps) => {
  const navigate = useNavigate();

  const [creatorEvents, setCreatorEvents] = useState<CalendarEventItemResponse[]>([]);
  const [guestEvents, setGuestEvents] = useState<CalendarEventItemResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const calendarDays = useMemo(() => getCalendarDays(year, month), [year, month]);

  useEffect(() => {
    const loadCalendar = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await dashboardApi.getCalendar(year, month);
        setCreatorEvents(response.creatorEvents);
        setGuestEvents(response.guestEvents);
      } catch {
        setError("Не удалось загрузить календарь");
      } finally {
        setIsLoading(false);
      }
    };

    void loadCalendar();
  }, [year, month]);

  const eventsByDate = useMemo(() => {
    const map = new Map<string, DayEvent[]>();

    creatorEvents.forEach((event) => {
      const key = formatDateKey(new Date(event.startsAt));
      const existing = map.get(key) ?? [];
      existing.push({ ...event, source: "creator" });
      map.set(key, existing);
    });

    guestEvents.forEach((event) => {
      const key = formatDateKey(new Date(event.startsAt));
      const existing = map.get(key) ?? [];
      existing.push({ ...event, source: "guest" });
      map.set(key, existing);
    });

    map.forEach((events, key) => {
      map.set(
        key,
        [...events].sort(
          (a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime()
        )
      );
    });

    return map;
  }, [creatorEvents, guestEvents]);

  const handleDayClick = (date: Date, isCurrentMonth: boolean) => {
    if (!isCurrentMonth) {
      return;
    }

    const dateKey = formatDateKey(date);
    const events = eventsByDate.get(dateKey) ?? [];

    if (events.length === 0) {
      navigate(`/events/create?date=${dateKey}`);
    }
  };

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-bold capitalize text-slate-900">{formatMonthTitle(year, month)}</h2>
          <p className="mt-1 text-sm text-slate-500">Ваши мероприятия и приглашения на выбранный месяц</p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onPrevMonth}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Назад
          </button>
          <button
            type="button"
            onClick={onNextMonth}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Вперед
          </button>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-4 text-sm text-slate-600">
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-full bg-emerald-500" />
          <span>Мои мероприятия</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-full bg-blue-500" />
          <span>Приглашения</span>
        </div>
      </div>

      {isLoading ? (
        <div className="flex h-72 items-center justify-center rounded-2xl bg-slate-50 text-sm text-slate-600">
          Загрузка календаря...
        </div>
      ) : error ? (
        <div className="flex h-72 items-center justify-center rounded-2xl border border-red-200 bg-red-50 px-4 text-sm text-red-700">
          {error}
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200">
          <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50">
            {weekDays.map((day) => (
              <div
                key={day}
                className="px-2 py-3 text-center text-xs font-semibold uppercase tracking-wide text-slate-500 sm:px-3"
              >
                {day}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7">
            {calendarDays.map(({ date, isCurrentMonth }) => {
              const dateKey = formatDateKey(date);
              const events = eventsByDate.get(dateKey) ?? [];

              return (
                <button
                  key={dateKey}
                  type="button"
                  onClick={() => handleDayClick(date, isCurrentMonth)}
                  className={[
                    "min-h-36 border-b border-r border-slate-200 p-2 text-left align-top transition sm:min-h-40 sm:p-3",
                    isCurrentMonth
                      ? "bg-white hover:bg-slate-50"
                      : "bg-slate-50/70 text-slate-400",
                  ].join(" ")}
                >
                  <div className="mb-2 flex items-center justify-between">
                    <span
                      className={[
                        "inline-flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold",
                        isCurrentMonth ? "text-slate-900" : "text-slate-400",
                      ].join(" ")}
                    >
                      {date.getDate()}
                    </span>
                  </div>

                  <div className="space-y-2">
                    {events.map((event) => {
                      const isCreatorEvent = event.source === "creator";

                      return (
                        <div
                          key={`${event.source}-${event.eventId}`}
                          onClick={(clickEvent) => {
                            clickEvent.stopPropagation();
                            if (isCreatorEvent) {
                              navigate(`/events/${event.eventId}/manage`);
                            } else if (event.guestToken) {
                              navigate(`/events/${event.eventId}/invite/${event.guestToken}`);
                            }
                          }}
                          className={[
                            "rounded-xl border px-2 py-2 text-xs",
                            isCreatorEvent
                              ? "border-emerald-200 bg-emerald-50 text-emerald-800 cursor-pointer hover:bg-emerald-100"
                              : "border-blue-200 bg-blue-50 text-blue-800 cursor-pointer hover:bg-blue-100",
                          ].join(" ")}
                        >
                          <div className="font-semibold">{formatEventTime(event.startsAt)}</div>
                          <div className="mt-1 line-clamp-2">{event.title}</div>
                        </div>
                      );
                    })}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </section>
  );
};

export default EventCalendar;